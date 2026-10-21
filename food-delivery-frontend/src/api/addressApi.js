
import api from "./axios";

export const getAddresses = () => {
  return api.get("/users/me/addresses");
};

export const getAddressById = async (id) => {
  const response = await getAddresses();

  const address = response.data.find(
      (item) => item.id === Number(id)
  );

  if (!address) {
    throw new Error("Address not found");
  }

  return {
    ...response,
    data: address,
  };
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
