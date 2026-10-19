import api from "./axios";

export const getAddresses = () => {
  return api.get("/users/me/addresses");
};

export const getAddressById = (id) => {
  return api.get("/users/me/addresses").then((response) => ({
    ...response,
    data: response.data.find((address) => address.id === id),
  }));
};

export const createAddress = (data) => {
  return api.post("/users/me/addresses", data);
};

export const updateAddress = (id, data) => {
  return api.put(`/users/me/addresses/${id}`, data);
};

export const deleteAddress = (id) => {
  return api.delete(`/users/me/addresses/${id}`);
};
