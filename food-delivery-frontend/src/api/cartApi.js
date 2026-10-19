import api from "./axios";

export const addToCart = (menuItemId, quantity = 1) => {
  return api.post("/cart/items", {
    menuItemId,
    quantity,
  });
};

export const getCart = () => {
  return api.get("/cart");
};

export const updateCartItem = (id, quantity) => {
  return api.patch(`/cart/items/${id}`, {
    quantity,
  });
};

export const removeCartItem = (id) => {
  return api.delete(`/cart/items/${id}`);
};

export const clearCart = () => {
  return api.delete("/cart");
};
