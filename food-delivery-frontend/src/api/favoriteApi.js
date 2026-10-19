import api from "./axios";
export const getFavorites = () => api.get("/users/me/favorites");
export const addFavorite = (restaurantId) => api.post("/users/me/favorites", { restaurantId });
export const removeFavorite = (restaurantId) => api.delete(`/users/me/favorites/${restaurantId}`);
