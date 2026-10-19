import api from "./axios";

export const getRestaurantMenu = (restaurantId) => {
  return api.get(`/menu-items/restaurant/${restaurantId}`);
};

export const searchMenu = (params = {}) => api.get("/menu-items/search", { params });

export const createMenuItem = (data) => {
  return api.post("/menu-items", data);
};

export const uploadMenuItemImage = (menuItemId, file) => {
  const formData = new FormData();
  formData.append("file", file);
  return api.post(`/menu-items/${menuItemId}/images`, formData);
};

