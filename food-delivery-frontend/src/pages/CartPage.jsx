import { useEffect, useState } from "react";

import MainLayout from "../layouts/MainLayout";

import Button from "../components/ui/Button";
import {
  getCart,
  updateCartItem,
  removeCartItem,
  clearCart,
} from "../api/cartApi";
import { useDispatch, useSelector } from "react-redux";

import { loadCart } from "../utils/loadCart";
import { Link } from "react-router-dom";

export default function CartPage() {
  const dispatch = useDispatch();

  const cart = useSelector((state) => state.cart);
  console.log("cart -> ",cart);

  const [loading, setLoading] = useState(true);

  useEffect(() => {
    const init = async () => {
      await loadCart(dispatch);
      setLoading(false);
    };

    init();
  }, []);

  const increaseQuantity = async (item) => {
    await updateCartItem(item.id, item.quantity + 1);

    await loadCart(dispatch);
  };

  const decreaseQuantity = async (item) => {
    if (item.quantity === 1) {
      await removeCartItem(item.id);
    } else {
      await updateCartItem(item.id, item.quantity - 1);
    }

    await loadCart(dispatch);
  };

  const handleClearCart = async () => {
    await clearCart();

    await loadCart(dispatch);
  };

  if (loading) {
    return (
      <MainLayout>
        <div className="p-10">Loading...</div>
      </MainLayout>
    );
  }

  return (
    <MainLayout>
      <section className="min-h-screen bg-slate-50 py-16">
        <div className="mx-auto max-w-5xl px-6">
          <h1 className="mb-10 text-4xl font-bold">Cart</h1>

          {cart?.items?.length === 0 && (
            <div className="rounded-xl bg-white p-6 shadow">Cart is empty</div>
          )}

          <div className="space-y-4">
            {cart?.items?.map((item) => (
              <div key={item.id} className="rounded-xl bg-white p-5 shadow">
                <div className="flex justify-between">
                  <div>
                    <h2 className="font-semibold">{item.itemName}</h2>

                    <p>₹{item.unitPrice}</p>
                  </div>

                  <div className="flex items-center gap-3">
                    <Button onClick={() => decreaseQuantity(item)}>-</Button>

                    <span>{item.quantity}</span>

                    <Button onClick={() => increaseQuantity(item)}>+</Button>
                  </div>
                </div>
              </div>
            ))}
          </div>

          <div className="mt-10 rounded-xl bg-white p-6 shadow">
            <h2 className="text-2xl font-bold">
              Total: ₹{cart?.total || 0}
            </h2>

            <div className="mt-4 flex gap-3">
              <Button onClick={handleClearCart}>Clear Cart</Button>
              {cart?.items?.length > 0 && (
                <Link to="/checkout">
                  <Button>Checkout</Button>
                </Link>
              )}
            </div>
          </div>
        </div>
      </section>
    </MainLayout>
  );
}
