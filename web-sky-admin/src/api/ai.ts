import request from '@/utils/request'

// AI生成菜品描述
export const generateDishDescription = (params: any) => {
  return request({
    url: '/admin/ai/generateDescription',
    method: 'post',
    data: params
  })
}

// AI生成套餐描述
export const generateSetmealDescription = (params: any) => {
  return request({
    url: '/admin/ai/generateSetmealDescription',
    method: 'post',
    data: params
  })
}
