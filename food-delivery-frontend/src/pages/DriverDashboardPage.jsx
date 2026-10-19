import { useEffect, useRef, useState } from "react";
import MainLayout from "../layouts/MainLayout";
import Button from "../components/ui/Button";
import { acceptDriverOrder, getAvailableDriverOrders, getDriverOrders, updateDriverLocation, updateDriverOrderStatus, updateDriverPresence } from "../api/driverApi";
import { subscribeToOrder } from "../socket";

const nextStatus = { OUT_FOR_DELIVERY: "DELIVERED" };

export default function DriverDashboardPage() {
  const [available, setAvailable] = useState([]);
  const [deliveries, setDeliveries] = useState([]);
  const [error, setError] = useState("");
  const [loading, setLoading] = useState(true);
  const [trackingOrderId, setTrackingOrderId] = useState(null);
  const [online, setOnline] = useState(false);
  const [presenceLocation, setPresenceLocation] = useState(null);
  const watchId = useRef(null);
  const lastSentLocation = useRef(null);
  const loadOrders = () => Promise.all([getAvailableDriverOrders(), getDriverOrders()]).then(([availableResponse, driverResponse]) => {
    setAvailable(availableResponse.data.content); setDeliveries(driverResponse.data.content);
  }).catch((requestError) => setError(requestError.response?.data?.message || "Unable to load driver deliveries.")).finally(() => setLoading(false));
  useEffect(() => { loadOrders(); }, []);
  useEffect(() => () => {
    if (watchId.current !== null) navigator.geolocation.clearWatch(watchId.current);
  }, []);
  useEffect(() => {
    const cleanup = deliveries.map((order) => subscribeToOrder(order.id, (updated) => setDeliveries((current) => current.map((item) => item.id === updated.id ? updated : item))));
    return () => cleanup.forEach((unsubscribe) => unsubscribe());
  }, [deliveries.length]);
  const accept = async (orderId) => { try { await acceptDriverOrder(orderId); await loadOrders(); } catch (requestError) { setError(requestError.response?.data?.message || "Unable to accept this order."); } };
  const toggleAvailability = () => {
    if (online) {
      if (!presenceLocation) return setError("A current location is needed to update your availability.");
      updateDriverPresence(presenceLocation.latitude,presenceLocation.longitude,false).then(()=>{setOnline(false);setError("");}).catch(()=>setError("Unable to go offline."));
      return;
    }
    if (!navigator.geolocation) return setError("Your browser does not support location sharing.");
    navigator.geolocation.getCurrentPosition(({coords})=>{
      const location={latitude:coords.latitude,longitude:coords.longitude};
      updateDriverPresence(location.latitude,location.longitude,true).then(()=>{setPresenceLocation(location);setOnline(true);setError("");return loadOrders();}).catch((e)=>setError(e.response?.data?.message||"Unable to go online."));
    },()=>setError("Location permission is required to go online."),{enableHighAccuracy:true,maximumAge:10000,timeout:15000});
  };
  const stopTracking = () => {
    if (watchId.current !== null) navigator.geolocation.clearWatch(watchId.current);
    watchId.current = null;
    setTrackingOrderId(null);
  };
  const shareLocation = (orderId) => {
    if (!navigator.geolocation) return setError("Your browser does not support location sharing.");
    stopTracking();
    lastSentLocation.current = null;
    watchId.current = navigator.geolocation.watchPosition(({ coords }) => {
      const now = Date.now();
      const previous = lastSentLocation.current;
      const distance = previous ? distanceInMeters(previous.latitude, previous.longitude, coords.latitude, coords.longitude) : Infinity;
      if (previous && now - previous.sentAt < 10000 && distance < 25) return;
      lastSentLocation.current = { latitude: coords.latitude, longitude: coords.longitude, sentAt: now };
      setPresenceLocation({latitude:coords.latitude,longitude:coords.longitude});
      updateDriverLocation(orderId, coords.latitude, coords.longitude).then(() => setError("")).catch(() => setError("Unable to send your location."));
    }, () => { setError("Location permission was not granted."); stopTracking(); }, { enableHighAccuracy: true, maximumAge: 10000, timeout: 15000 });
    setTrackingOrderId(orderId);
  };

  const distanceInMeters = (fromLatitude, fromLongitude, toLatitude, toLongitude) => {
    const radians = (value) => value * Math.PI / 180;
    const earthRadius = 6371000;
    const latitudeDelta = radians(toLatitude - fromLatitude);
    const longitudeDelta = radians(toLongitude - fromLongitude);
    const a = Math.sin(latitudeDelta / 2) ** 2 + Math.cos(radians(fromLatitude)) * Math.cos(radians(toLatitude)) * Math.sin(longitudeDelta / 2) ** 2;
    return 2 * earthRadius * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
  };
  const updateStatus = async (order) => { try { await updateDriverOrderStatus(order.id, nextStatus[order.status]); if (trackingOrderId === order.id) stopTracking(); await loadOrders(); } catch (requestError) { setError(requestError.response?.data?.message || "Unable to update delivery status."); } };
  const orderCard = (order, action) => <article key={order.id} className="rounded-xl bg-white p-5 shadow"><div className="flex justify-between gap-3"><div><h2 className="font-bold">{order.orderNumber}</h2><p className="text-sm text-slate-500">{order.deliveryAddress}</p></div><span className="text-sm font-semibold text-orange-600">{order.status}</span></div><ul className="mt-3 text-sm text-slate-600">{order.items.map((item) => <li key={item.id}>{item.itemName} × {item.quantity}</li>)}</ul><p className="mt-3 font-semibold">₹{order.totalAmount}</p>{action}</article>;
  return <MainLayout><section className="mx-auto min-h-screen max-w-6xl px-6 py-12"><h1 className="mb-3 text-4xl font-bold">Driver Dashboard</h1><p className="mb-4 text-slate-600">Go online to let the system match you with the closest ready delivery.</p><Button className="mb-6" onClick={toggleAvailability}>{online?"Go offline":"Go online and share location"}</Button>{error && <p className="mb-5 rounded-lg bg-red-50 p-3 text-red-600">{error}</p>}
    {loading ? <p>Loading deliveries...</p> : <div className="grid gap-8 lg:grid-cols-2"><div><h2 className="mb-4 text-2xl font-bold">Available Deliveries</h2><p className="mb-3 text-sm text-slate-500">Only orders prepared and marked ready by restaurants appear here.</p><div className="space-y-4">{available.length ? available.map((order) => orderCard(order, <Button className="mt-4" onClick={() => accept(order.id)}>Accept delivery</Button>)) : <p>No orders are ready for pickup.</p>}</div></div><div><h2 className="mb-4 text-2xl font-bold">My Deliveries</h2><div className="space-y-4">{deliveries.length ? deliveries.map((order) => orderCard(order, <div className="mt-4 flex flex-wrap gap-3">{order.status === "OUT_FOR_DELIVERY" && (trackingOrderId === order.id ? <Button onClick={stopTracking}>Stop location sharing</Button> : <Button onClick={() => shareLocation(order.id)}>Start live location</Button>)}{nextStatus[order.status] && <Button onClick={() => updateStatus(order)}>Mark delivered</Button>}</div>)) : <p>No deliveries assigned to you.</p>}</div></div></div>}
  </section></MainLayout>;
}
