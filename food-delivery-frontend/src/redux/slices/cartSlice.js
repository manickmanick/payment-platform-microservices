import { createSlice } from "@reduxjs/toolkit";

const cartSlice = createSlice({
  name: "cart",

  initialState: {
    items: [],
    totalAmount: 0,
    count: 0,
  },

  reducers: {
    setCart: (
      state,
      action
    ) => {
      state.items =
        action.payload.items;

      state.total =
        action.payload.total;

      state.count =
        action.payload.items.reduce(
          (total, item) =>
            total + item.quantity,
          0
        );
    },

    clearCartState:
      (state) => {
        state.items = [];
        state.totalAmount = 0;
        state.count = 0;
      },
  },
});

export const {
  setCart,
  clearCartState,
} = cartSlice.actions;

export default cartSlice.reducer;