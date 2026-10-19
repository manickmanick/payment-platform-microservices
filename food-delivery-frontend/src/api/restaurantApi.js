import api from "./axios";

export const getRestaurants = (params = {}) => {
  return api.get("/restaurants/search", { params });
};

export const getRestaurantReviews = (id) => api.get(`/restaurants/${id}/reviews`);
export const addRestaurantReview = (id, data) => api.post(`/restaurants/${id}/reviews`, data);

export const getRestaurant = (id) => {
  return api.get(`/restaurants/${id}`);
};

export const createRestaurant = (data) => {
  return api.post("/restaurants", data);
};

export const uploadRestaurantImage = (restaurantId, file) => {
  const formData = new FormData();
  formData.append("file", file);
  return api.post(`/restaurants/${restaurantId}/image`, formData);
};
