package com.sky.service.impl;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import com.github.xiaoymin.knife4j.core.util.CollectionUtils;
import com.sky.constant.MessageConstant;
import com.sky.context.BaseContext;
import com.sky.dto.OrdersCancelDTO;
import com.sky.dto.OrdersConfirmDTO;
import com.sky.dto.OrdersPageQueryDTO;
import com.sky.dto.OrdersPaymentDTO;
import com.sky.dto.OrdersRejectionDTO;
import com.sky.dto.OrdersSubmitDTO;
import com.sky.entity.AddressBook;
import com.sky.entity.OrderDetail;
import com.sky.entity.Orders;
import com.sky.entity.ShoppingCart;
import com.sky.entity.User;
import com.sky.exception.AddressBookBusinessException;
import com.sky.exception.OrderBusinessException;
import com.sky.exception.ShoppingCartBusinessException;
import com.sky.mapper.AddressBookMapper;
import com.sky.mapper.OrderDetailMapper;
import com.sky.mapper.OrderMapper;
import com.sky.mapper.ShoppingCartMapper;
import com.sky.mapper.UserMapper;
import com.sky.result.PageResult;
import com.sky.service.OrderService;
import com.sky.utils.HttpClientUtil;
import com.sky.utils.WeChatPayUtil;
import com.sky.vo.OrderPaymentVO;
import com.sky.vo.OrderStatisticsVO;
import com.sky.vo.OrderSubmitVO;
import com.sky.vo.OrderVO;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class OrderServiceimpl implements OrderService {

    @Value("${sky.shop.address}")
    private String shopAddress;

    @Value("${sky.baidu.ak}")
    private String ak;
    @Autowired
    private OrderMapper orderMapper;
    @Autowired
    private OrderDetailMapper orderDetailMapper;
    @Autowired
    private ShoppingCartMapper shoppingCartMapper;
    @Autowired
    private UserMapper userMapper;
    @Autowired
    private AddressBookMapper addressBookMapper;
    @Autowired
    private WeChatPayUtil weChatPayUtil;

    /**
     * 校验收货地址是否超出配送范围
     * @param address
     */
    private void checkOutOfRange(String address) {
        Map map = new HashMap();
        map.put("address",shopAddress);
        map.put("output","json");
        map.put("ak",ak);

        //获取店铺的经纬度坐标
        String shopCoordinate = HttpClientUtil.doGet("https://api.map.baidu.com/geocoding/v3", map);

        JSONObject jsonObject = JSON.parseObject(shopCoordinate);
        if(!jsonObject.getString("status").equals("0")){
            throw new OrderBusinessException("店铺地址解析失败");
        }

        //数据解析
        JSONObject location = jsonObject.getJSONObject("result").getJSONObject("location");
        String lat = location.getString("lat");
        String lng = location.getString("lng");
        //店铺经纬度坐标
        String shopLngLat = lat + "," + lng;

        map.put("address",address);
        //获取用户收货地址的经纬度坐标
        String userCoordinate = HttpClientUtil.doGet("https://api.map.baidu.com/geocoding/v3", map);

        jsonObject = JSON.parseObject(userCoordinate);
        if(!jsonObject.getString("status").equals("0")){
            throw new OrderBusinessException("收货地址解析失败");
        }

        //数据解析
        location = jsonObject.getJSONObject("result").getJSONObject("location");
        lat = location.getString("lat");
        lng = location.getString("lng");
        //用户收货地址经纬度坐标
        String userLngLat = lat + "," + lng;

        map.put("origin",shopLngLat);
        map.put("destination",userLngLat);
        map.put("steps_info","0");

        //路线规划
        String json = HttpClientUtil.doGet("https://api.map.baidu.com/directionlite/v1/driving", map);

        jsonObject = JSON.parseObject(json);
        if(!jsonObject.getString("status").equals("0")){
            throw new OrderBusinessException("配送路线规划失败");
        }

        //数据解析
        JSONObject result = jsonObject.getJSONObject("result");
        JSONArray jsonArray = (JSONArray) result.get("routes");
        Integer distance = (Integer) ((JSONObject) jsonArray.get(0)).get("distance");

        if(distance > 5000){
            //配送距离超过5000米
            throw new OrderBusinessException("超出配送范围");
        }
    }
    /**
     * 用户下单
     * @param ordersSubmitDTO
     * @return
     */
    @Override
    @Transactional
    public OrderSubmitVO submitOrder(OrdersSubmitDTO ordersSubmitDTO) {
        //处理各种业务异常
        //判断一下用户的地址是否为空(地址薄)，如果为空则抛出一个异常，提示用户请先添加地址
        AddressBook addressBook = addressBookMapper.getById(ordersSubmitDTO.getAddressBookId());
        if(addressBook == null){
            throw new AddressBookBusinessException(MessageConstant.ADDRESS_BOOK_IS_NULL);
        }
        //校验用户的收货地址是否超出配送范围
        checkOutOfRange(addressBook.getCityName() + addressBook.getDistrictName() + addressBook.getDetail());
        //判断购物车数据是否为空(购物车)，如果为空则抛出一个异常，提示用户请先添加购物车数据
        Long useId = BaseContext.getCurrentId();
        ShoppingCart shoppingCart = new ShoppingCart();
        shoppingCart.setId(useId);
        List<ShoppingCart> shoppingCartList = shoppingCartMapper.list(shoppingCart);
        if(shoppingCartList == null || shoppingCartList.size() == 0){
            throw new ShoppingCartBusinessException(MessageConstant.SHOPPING_CART_IS_NULL);
        }
        //向订单表插入一条数据(一个订单)
        Orders orders = new Orders();
        BeanUtils.copyProperties(ordersSubmitDTO, orders);
        orders.setOrderTime(LocalDateTime.now());
        orders.setPayStatus(Orders.UN_PAID);
        orders.setStatus(Orders.PENDING_PAYMENT);
        orders.setNumber(String.valueOf(System.currentTimeMillis()));
        orders.setPhone(addressBook.getPhone());
        orders.setConsignee(addressBook.getConsignee());//收货人
        orders.setUserId(useId);
        orders.setAddress(addressBook.getDetail());

        orderMapper.insert(orders);
        //向订单明细表批量插入多条数据(多个订单明细)
        List<OrderDetail> orderDetailList = new ArrayList<>();
        for(ShoppingCart cart : shoppingCartList){
            OrderDetail orderDetail = new OrderDetail();
            BeanUtils.copyProperties(cart, orderDetail);
            orderDetail.setOrderId(orders.getId());//设置当前订单明细相关联的订单id
            orderDetailList.add(orderDetail);
        }
        orderDetailMapper.insertBatch(orderDetailList);
        //清空购物车
        shoppingCartMapper.deleteByUserId(useId);
        //封装VO 返回订单提交结果  
        OrderSubmitVO orderSubmitVO = OrderSubmitVO.builder()
                            .id(orders.getId())
                            .orderTime(orders.getOrderTime())
                            .orderAmount(orders.getAmount())
                            .orderNumber(orders.getNumber())
                            .build();
        return orderSubmitVO;
    }

    /**
     * 订单支付
     *
     * @param ordersPaymentDTO
     * @return
     
    public OrderPaymentVO payment(OrdersPaymentDTO ordersPaymentDTO) throws Exception {
        // 当前登录用户id
        Long userId = BaseContext.getCurrentId();
        User user = userMapper.getById(userId);

        /* //调用微信支付接口，生成预支付交易单
        JSONObject jsonObject = weChatPayUtil.pay(
                ordersPaymentDTO.getOrderNumber(), //商户订单号
                new BigDecimal(0.01), //支付金额，单位 元
                "苍穹外卖订单", //商品描述
                user.getOpenid() //微信用户的openid
        );

        if (jsonObject.getString("code") != null && jsonObject.getString("code").equals("ORDERPAID")) {
            throw new OrderBusinessException("该订单已支付");
        } 

        JSONObject jsonObject = new JSONObject();
        jsonObject.put("code","ORDERPAID");
        OrderPaymentVO vo = jsonObject.toJavaObject(OrderPaymentVO.class);
        vo.setPackageStr(jsonObject.getString("package"));
        Integer OrderPaidStatus = Orders.PAID;//支付状态，已支付
        Integer OrderStatus = Orders.TO_BE_CONFIRMED;  //订单状态，待接单
        LocalDateTime check_out_time = LocalDateTime.now();//更新支付时间
        orderMapper.updateStatus(OrderStatus, OrderPaidStatus, check_out_time, this.orders.getId());
        return vo;

         OrderPaymentVO vo = jsonObject.toJavaObject(OrderPaymentVO.class);
        vo.setPackageStr(jsonObject.getString("package"));

        return vo; 
    } */
   @Override
    public OrderPaymentVO payment(OrdersPaymentDTO ordersPaymentDTO) throws Exception {
        Long userId = BaseContext.getCurrentId();
        User user = userMapper.getById(userId);

        // 1. 注释掉原来的微信支付调用
        /*
        JSONObject jsonObject = weChatPayUtil.pay(
                ordersPaymentDTO.getOrderNumber(), // 商户订单号
                new BigDecimal(0.01), // 支付金额，单位 元
                "苍穹外卖订单", // 商品描述
                user.getOpenid() // 微信用户的openid
        );

        if (jsonObject.getString("code") != null && jsonObject.getString("code").equals("ORDERPAID")) {
            throw new OrderBusinessException("该订单已支付");
        }
        */

        // 2. 模拟支付成功：创建JSONObject并设置code
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("code", "ORDERPAID");

        // 3. 模拟返回前端需要的OrderPaymentVO
        OrderPaymentVO vo = new OrderPaymentVO();
        vo.setNonceStr(UUID.randomUUID().toString());
        vo.setPackageStr("prepay_id=mock_prepay_id");
        vo.setPaySign("mock_pay_sign");
        vo.setSignType("RSA");
        vo.setTimeStamp(String.valueOf(System.currentTimeMillis() / 1000));

        // 4. 更新订单状态为已支付、待接单
        Orders order = orderMapper.getByNumber(ordersPaymentDTO.getOrderNumber());
        order.setStatus(Orders.TO_BE_CONFIRMED);    // 订单状态：待接单
        order.setPayStatus(Orders.PAID);             // 支付状态：已支付
        order.setOrderTime(LocalDateTime.now());       // 支付时间
        order.setCheckoutTime(LocalDateTime.now());  // 结账时间
        orderMapper.update(order);

        return vo;
    }

    /**
     * 支付成功，修改订单状态
     *
     * @param outTradeNo
     */
    public void paySuccess(String outTradeNo) {

        // 根据订单号查询订单
        Orders ordersDB = orderMapper.getByNumber(outTradeNo);

        // 根据订单id更新订单的状态、支付方式、支付状态、结账时间
        Orders orders = Orders.builder()
                .id(ordersDB.getId())
                .status(Orders.TO_BE_CONFIRMED)
                .payStatus(Orders.PAID)
                .checkoutTime(LocalDateTime.now())
                .build();

        orderMapper.update(orders);
    }
    /**
     * 历史订单查询
     * @param page
     * @param pageSize
     * @param status
     * @return
     */
    @Override
    public PageResult pageQuery(int page,int pageSize,Integer status) {

        PageHelper.startPage(page,pageSize);
        OrdersPageQueryDTO ordersPageQueryDTO = new OrdersPageQueryDTO();
        ordersPageQueryDTO.setUserId(BaseContext.getCurrentId());
        ordersPageQueryDTO.setStatus(status);
        log.info("pageQuery: page={}, pageSize={}, status={}, userId={}", page, pageSize, status, BaseContext.getCurrentId());
        Page<Orders> page1 = orderMapper.pageQuery(ordersPageQueryDTO);
        log.info("pageQuery result: total={}, size={}", page1.getTotal(), page1.getResult().size());
        List<OrderVO> list = new ArrayList<>();
        if(page1 != null && page1.getTotal() > 0){
            for(Orders orders:page1){
                Long orderId = orders.getId();
                List<OrderDetail> orderDetails = orderDetailMapper.getByOrderId(orderId);
                log.info("orderId={}, orderDetail size={}", orderId, orderDetails.size());
                OrderVO orderVO = new OrderVO();
                BeanUtils.copyProperties(orders, orderVO);
                orderVO.setOrderDetailList(orderDetails);
                list.add(orderVO);
            }
        }
        log.info("pageQuery final: list size={}", list.size());
        return new PageResult(page1.getTotal(), list);
    }
    /**
     * 查询订单详情
     * @param id
     * @return
     */
    @Override
    public OrderVO details(Long id) {
        //根据id查询订单
        Orders orders = orderMapper.getById(id);
        //查询该订单对应的菜品/套餐明细
        List<OrderDetail> orderDetailList = orderDetailMapper.getByOrderId(id);

        //将该订单及其详情封装到OrderVO并返回
        OrderVO orderVO = new OrderVO();
        BeanUtils.copyProperties(orders, orderVO);
        orderVO.setOrderDetailList(orderDetailList);
        return orderVO;
    }
    /**
     * 取消订单
     * @param id
     */
    @Override
    public void userCancelById(Long id)throws Exception {
        //根据id查询订单
        Orders ordersDB = orderMapper.getById(id);
        //判断订单是否存在
        if(ordersDB == null){
            throw new OrderBusinessException(MessageConstant.ACCOUNT_NOT_FOUND);
        }
        //订单状态 1待付款 2待接单 3已接单 4派送中 5已完成 6已取消
        //仅1、2状态下能退款
        if(ordersDB.getStatus() > 2){
            throw new OrderBusinessException(MessageConstant.ORDER_STATUS_ERROR);
        }
        Orders orders = new Orders();
        orders.setId(ordersDB.getId());
        //订单处于待接单状态下取消，需要进行退款
        if(ordersDB.getStatus().equals(Orders.TO_BE_CONFIRMED)){
            //调用微信支付退款接口
            weChatPayUtil.refund(
                    ordersDB.getNumber(), //商户订单号
                    ordersDB.getNumber(), //商户退款单号
                    new BigDecimal(0.01),//退款金额，单位 元
                    new BigDecimal(0.01));//原订单金额
            //将支付状态修改为已退款
            orders.setPayStatus(Orders.REFUND);
        }
        //更新订单状态、取消原因、取消时间
        orders.setStatus(Orders.CANCELLED);
        orders.setCancelTime(LocalDateTime.now());
        orders.setCancelReason("用户取消");
        orderMapper.update(orders);
    }
    /**
     * 再来一单
     * @param id
     */
    @Override
    public void repetition(Long id) {
       //查询当前用户id
       Long userId = BaseContext.getCurrentId();
       //根据订单id查询当前订单详情
       List<OrderDetail> orderDetailList = orderDetailMapper.getByOrderId(id);
       //将订单详情对象转化为购物车对象
        List<ShoppingCart> shoppingCartList = orderDetailList.stream().map(x -> {
            ShoppingCart shoppingCart = new ShoppingCart();

            // 将原订单详情里面的菜品信息重新复制到购物车对象中
            BeanUtils.copyProperties(x, shoppingCart, "id");
            shoppingCart.setUserId(userId);
            shoppingCart.setCreateTime(LocalDateTime.now());

            return shoppingCart;
        }).collect(Collectors.toList());

        // 将购物车对象批量添加到数据库
        shoppingCartMapper.insertBatch(shoppingCartList);
    }
    /**
     * 订单搜索
     * @param ordersPageQueryDTO
     * @return
     */
    @Override
    public PageResult conditionSearch(OrdersPageQueryDTO ordersPageQueryDTO) {
        PageHelper.startPage(ordersPageQueryDTO.getPage(),ordersPageQueryDTO.getPageSize());
        Page<Orders> page = orderMapper.pageQuery(ordersPageQueryDTO);
        Long total = page.getTotal();
        List<OrderVO> orderVOList = getOrderVOList(page);
        return new PageResult(total,orderVOList);
    }
    private List<OrderVO> getOrderVOList(Page<Orders> page){
        List<Orders> ordersList = page.getResult();
        List<OrderVO> orderVOList = new ArrayList<>();
        if(!CollectionUtils.isEmpty(ordersList)){
            for(Orders  orders : ordersList){
                OrderVO orderVO = new OrderVO();
                //将共同字段复制到OrderVO
                BeanUtils.copyProperties(orders, orderVO);
                String orderDishes = getOrderDishesStr(orders);
                //将订单菜品信息封装到orderVO中,并添加到orderVOList
                orderVO.setOrderDishes(orderDishes);
                orderVOList.add(orderVO);
            }
        }
        return orderVOList;
    }
    /**
     * 根据订单id获取菜品信息字符串
     *
     * @param orders
     * @return
     */
    private String getOrderDishesStr(Orders orders) {
        // 查询订单菜品详情信息（订单中的菜品和数量）
        List<OrderDetail> orderDetailList = orderDetailMapper.getByOrderId(orders.getId());

        // 将每一条订单菜品信息拼接为字符串（格式：宫保鸡丁*3；）
        List<String> orderDishList = orderDetailList.stream().map(x -> {
            String orderDish = x.getName() + "*" + x.getNumber() + ";";
            return orderDish;
        }).collect(Collectors.toList());

        // 将该订单对应的所有菜品信息拼接在一起
        return String.join("", orderDishList);
    }
    /**
     * 各个状态的订单数量统计
     * @return
     */
    @Override
    public OrderStatisticsVO statistics() {
        OrderStatisticsVO orderStatisticsVO = new OrderStatisticsVO();
        Integer toBeConfirmed  = orderMapper.countStatus(Orders.TO_BE_CONFIRMED);
        Integer confirmed = orderMapper.countStatus(Orders.CONFIRMED);
        Integer deliverInProgress  = orderMapper.countStatus(Orders.DELIVERY_IN_PROGRESS);
        orderStatisticsVO.setConfirmed(toBeConfirmed);
        orderStatisticsVO.setConfirmed(confirmed);
        orderStatisticsVO.setDeliveryInProgress(deliverInProgress);
        return orderStatisticsVO;
    }
    /**
     * 接单
     * @param ordersConfirmDTO
     */
    public void confirm(OrdersConfirmDTO ordersConfirmDTO){
        Orders orders = Orders.builder()
                        .id(ordersConfirmDTO.getId())
                        .status(Orders.CONFIRMED)
                        .build();
        orderMapper.update(orders);
    }
    /**
     * 拒单
     * @param ordersRejectionDTO
     */
    public void rejection(OrdersRejectionDTO ordersRejectionDTO) throws Exception{
        //根据id查询订单详情
        Orders ordersDB = orderMapper.getById(ordersRejectionDTO.getId());
        //只有订单处于“待接单”状态时可以执行拒单操作
        if(ordersDB == null || !ordersDB.getStatus().equals(Orders.TO_BE_CONFIRMED)){
            throw new OrderBusinessException(MessageConstant.ORDER_STATUS_ERROR);
        }
        //商家拒单时，如果用户已经完成了支付，需要为用户退款
        Integer payStatus = ordersDB.getPayStatus();
        if(payStatus == Orders.PAID){
            //用户已支付，需要退款（模拟退款，跳过微信API）
            log.info("模拟退款：订单号={}, 金额={}", ordersDB.getNumber(), ordersDB.getAmount());
        }
        //商家拒单其实就是将订单状态修改为“已取消”,商家拒单时需要指定拒单原因
        Orders orders = Orders.builder()
                        .id(ordersRejectionDTO.getId())
                        .cancelReason(ordersRejectionDTO.getRejectionReason())
                        .status(Orders.CANCELLED)
                        .cancelTime(LocalDateTime.now())
                        .build();
        orderMapper.update(orders);
    }
    /**
     * 取消订单
     * @param ordersCancelDTO
     */
    public void cancel(OrdersCancelDTO ordersCancelDTO) throws Exception{
        //根据id查询订单详情
        Orders ordersDB = orderMapper.getById(ordersCancelDTO.getId());
        //支付状态
        Integer payStatus = ordersDB.getPayStatus();
        if(payStatus == Orders.PAID){
            //用户已支付，需要退款（模拟退款，跳过微信API）
            log.info("模拟退款：订单号={}, 金额={}", ordersDB.getNumber(), ordersDB.getAmount());
        }
        //更新订单状态、取消原因、取消时间
        Orders orders = Orders.builder()
                        .id(ordersCancelDTO.getId())
                        .cancelReason(ordersCancelDTO.getCancelReason())
                        .status(Orders.CANCELLED)
                        .cancelTime(LocalDateTime.now())
                        .build();
        orderMapper.update(orders);
    }
    /**
     * 派送订单
     * @param id
     */
    public void delivery(Long id) {
        // 根据id查询订单
        Orders ordersDB = orderMapper.getById(id);
        // 校验订单是否存在，并且状态为3
        if (ordersDB == null || !ordersDB.getStatus().equals(Orders.CONFIRMED)) {
            throw new OrderBusinessException(MessageConstant.ORDER_STATUS_ERROR);
        }
        Orders orders = new Orders();
        orders.setId(ordersDB.getId());
        // 更新订单状态,状态转为派送中
        orders.setStatus(Orders.DELIVERY_IN_PROGRESS);
        orderMapper.update(orders);
    }
    /**
     * 完成订单
     *
     * @param id
     */
    public void complete(Long id) {
        // 根据id查询订单
        Orders ordersDB = orderMapper.getById(id);

        // 校验订单是否存在，并且状态为4
        if (ordersDB == null || !ordersDB.getStatus().equals(Orders.DELIVERY_IN_PROGRESS)) {
            throw new OrderBusinessException(MessageConstant.ORDER_STATUS_ERROR);
        }

        Orders orders = new Orders();
        orders.setId(ordersDB.getId());
        // 更新订单状态,状态转为完成
        orders.setStatus(Orders.COMPLETED);
        orders.setDeliveryTime(LocalDateTime.now());

        orderMapper.update(orders);
    }
}
