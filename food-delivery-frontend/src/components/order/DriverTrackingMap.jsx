import { useEffect, useState } from "react";
import { CircleMarker, MapContainer, Popup, TileLayer, useMap } from "react-leaflet";
import "leaflet/dist/leaflet.css";
import { subscribeToDriverLocation } from "../../socket";

function Recenter({ location }) {
  const map = useMap();
  useEffect(() => { map.setView([location.latitude, location.longitude], Math.max(map.getZoom(), 15)); }, [location, map]);
  return null;
}

export default function DriverTrackingMap({ orderId }) {
  const [location, setLocation] = useState(null);
  useEffect(() => {
    return subscribeToDriverLocation(orderId, setLocation);
  }, [orderId]);
  if (!location) return <p className="mt-4 text-sm text-slate-500">Waiting for the driver to share their location.</p>;
  return <div className="mt-5 overflow-hidden rounded-xl border"><MapContainer center={[location.latitude, location.longitude]} zoom={15} className="h-64 w-full" scrollWheelZoom={false}>
    <TileLayer attribution='&copy; <a href="https://www.openstreetmap.org/copyright">OpenStreetMap</a> contributors' url="https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png" />
    <CircleMarker center={[location.latitude, location.longitude]} radius={10} pathOptions={{ color: "#ea580c", fillColor: "#fb923c", fillOpacity: 1 }}><Popup>Driver location<br />Updated {new Date(location.updatedAt).toLocaleTimeString()}</Popup></CircleMarker>
    <Recenter location={location} />
  </MapContainer></div>;
}
