import { useEffect, useState } from "react";
import { useNavigate } from "react-router-dom";

import MainLayout from "../layouts/MainLayout";
import Button from "../components/ui/Button";

import { getAddresses } from "../api/addressApi";
import { getCart } from "../api/cartApi";
import { quoteCoupon } from "../api/promotionApi";

export default function CheckoutPage() {
  const navigate = useNavigate();

  const [addresses, setAddresses] = useState([]);
  const [selectedAddress, setSelectedAddress] = useState(null);
  const [cart, setCart] = useState(null);
  const [couponCode, setCouponCode] = useState("");
  const [couponDiscount, setCouponDiscount] = useState(0);
  const [couponMessage, setCouponMessage] = useState("");
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    loadData();
  }, []);

  const loadData = async () => {
    try {
      const [addressResponse, cartResponse] = await Promise.all([
        getAddresses(),
        getCart(),
      ]);

      setAddresses(addressResponse.data);
      setCart(cartResponse.data);
    } catch (error) {
      console.error("Error loading checkout data:", error);
    } finally {
      setLoading(false);
    }
  };

  const handleContinue = () => {
    if (!selectedAddress) return;
    const proceed = (coordinates = null) => navigate("/payment", { state: {
        deliveryAddress: [selectedAddress.line1, selectedAddress.line2, selectedAddress.city, selectedAddress.state, selectedAddress.postalCode, selectedAddress.country].filter(Boolean).join(", "),
        amount: Math.max(0, Number(cart.total)-couponDiscount) + (Number(cart.total)>=499?0:49) + Math.max(0,(Number(cart.total)-couponDiscount)*0.05),
        couponCode: couponCode.trim().toUpperCase(),
        coordinates,
        customerPhone: selectedAddress.phoneNumber || "",
        smsOptIn: Boolean(selectedAddress.smsOptIn),
      }});
    if (navigator.geolocation) navigator.geolocation.getCurrentPosition((position)=>proceed({latitude:position.coords.latitude,longitude:position.coords.longitude}),()=>proceed());
    else proceed();
  };

  const applyCoupon = async () => {
    setCouponMessage("");setCouponDiscount(0);
    try { const { data } = await quoteCoupon(couponCode.trim().toUpperCase(), cart?.total || 0); setCouponDiscount(Number(data.discount || 0)); setCouponMessage("Coupon applied."); }
    catch (e) { setCouponMessage(e.response?.data?.message || "Coupon could not be applied."); }
  };

  if (loading) {
    return (
      <MainLayout>
        <div className="flex min-h-[60vh] items-center justify-center text-lg font-medium text-gray-500">
          Loading checkout summary...
        </div>
      </MainLayout>
    );
  }

  return (
    <MainLayout>
      <section className="bg-slate-50 py-12">
        <div className="mx-auto max-w-6xl px-6">
          <h1 className="mb-10 text-4xl font-bold">Checkout</h1>

          <div className="grid gap-8 lg:grid-cols-2">
            {/* Address Section */}
            <div className="rounded-2xl bg-white p-6 shadow">
              <h2 className="mb-6 text-2xl font-bold">Delivery Address</h2>

              <div className="space-y-4">
                {addresses.length === 0 ? (
                  <p className="text-gray-500 py-4">
                    No addresses found. Please add an address to continue.
                  </p>
                ) : (
                  addresses.map((address) => (
                    <div
                      key={address.id}
                      onClick={() => setSelectedAddress(address)}
                      className={`cursor-pointer rounded-xl border p-4 transition duration-200 ${
                        selectedAddress?.id === address.id
                          ? "border-orange-500 bg-orange-50 ring-2 ring-orange-500/20"
                          : "border-gray-200 hover:border-gray-300"
                      }`}
                    >
                      <h3 className="font-semibold text-gray-800">
                        {address.label}
                      </h3>
                      <p className="text-gray-600 mt-1">
                        {address.line1}
                      </p>
                      {address.line2 && (
                        <p className="text-gray-600">{address.line2}</p>
                      )}
                      <p className="text-gray-600">
                        {address.city}, {address.state}
                      </p>
                      <p className="text-gray-600 font-medium">
                        {address.postalCode}
                      </p>
                    </div>
                  ))
                )}
              </div>
            </div>

            {/* Order Summary */}
            <div className="h-fit rounded-2xl bg-white p-6 shadow">
              <h2 className="mb-6 text-2xl font-bold">Order Summary</h2>

              <div className="space-y-4">
                {cart?.items?.map((item) => (
                  <div
                    key={item.id}
                    className="flex justify-between border-b border-gray-100 pb-3"
                  >
                    <div>
                      <p className="font-medium text-gray-800">
                        {item.menuItemName}
                      </p>
                      <p className="text-sm text-gray-500 mt-0.5">
                        Qty: {item.quantity}
                      </p>
                    </div>
                    <p className="font-semibold text-gray-800">
                      ₹{item.subtotal}
                    </p>
                  </div>
                ))}
              </div>

              <div className="mt-6 border-t pt-6">
                <label className="mb-2 block text-sm font-medium text-gray-700" htmlFor="coupon-code">Coupon code</label>
                <div className="mb-4 flex gap-2"><input id="coupon-code" value={couponCode} onChange={(event) => { setCouponCode(event.target.value); setCouponDiscount(0); setCouponMessage(""); }} maxLength={40} placeholder="Enter coupon code (optional)" className="w-full rounded-lg border border-gray-300 px-3 py-2" /><button type="button" onClick={applyCoupon} disabled={!couponCode.trim()} className="rounded-lg border px-4 py-2 disabled:opacity-50">Apply</button></div>
                {couponMessage&&<p role="status" className="mb-3 text-sm text-slate-600">{couponMessage}</p>}
                <div className="space-y-2 text-sm"><div className="flex justify-between"><span>Subtotal</span><span>₹{cart?.total||0}</span></div><div className="flex justify-between"><span>Discount</span><span>-₹{couponDiscount.toFixed(2)}</span></div><div className="flex justify-between"><span>Delivery fee</span><span>₹{Number(cart?.total||0)>=499?"0.00":"49.00"}</span></div><div className="flex justify-between"><span>Estimated tax (5%)</span><span>₹{((Number(cart?.total||0)-couponDiscount)*0.05).toFixed(2)}</span></div></div>
                <div className="mt-3 flex justify-between border-t pt-3 text-xl font-bold text-gray-900"><span>Estimated total</span><span>₹{(Math.max(0,Number(cart?.total||0)-couponDiscount)+Math.max(0,Number(cart?.total||0)>=499?0:49)+Math.max(0,(Number(cart?.total||0)-couponDiscount)*0.05)).toFixed(2)}</span></div>

                {/* Helpful Validation Message */}
                {!selectedAddress && (
                  <p className="mt-4 text-center text-sm font-medium text-red-500 animate-pulse bg-red-50 py-2 rounded-lg border border-red-100">
                    ⚠️ Please select a delivery address to proceed
                  </p>
                )}

                <Button
                  className="mt-4 w-full"
                  onClick={handleContinue}
                  disabled={!selectedAddress}
                >
                  Continue To Payment
                </Button>
              </div>
            </div>
          </div>
        </div>
      </section>
    </MainLayout>
  );
}
