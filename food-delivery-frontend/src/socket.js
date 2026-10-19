import api from "./api/axios";

// The current backend exposes REST endpoints, so update live screens with
// authenticated polling rather than opening a socket without a server broker.
export function subscribeToOrder(orderId, onUpdate) {
  let active = true;
  let lastSignature = "";
  const refresh = async () => {
    try {
      const { data } = await api.get(`/orders/${orderId}`);
      const signature = `${data.status}:${data.updatedAt || ""}`;
      if (active && signature !== lastSignature) {
        lastSignature = signature;
        onUpdate(data);
      }
    } catch { /* transient network errors are retried on the next poll */ }
  };
  refresh();
  const timer = window.setInterval(refresh, 10000);
  return () => { active = false; window.clearInterval(timer); };
}

export function subscribeToNotifications(_userId, onNotification) {
  let active = true;
  const seen = new Set();
  let initialized = false;
  const refresh = async () => {
    try {
      const { data } = await api.get("/notifications");
      if (!active) return;
      const rows = data.content || [];
      for (const row of rows) {
        if (seen.has(row.id)) continue;
        seen.add(row.id);
        if (initialized) onNotification(row);
      }
      initialized = true;
    } catch { /* transient network errors are retried on the next poll */ }
  };
  refresh();
  const timer = window.setInterval(refresh, 15000);
  return () => { active = false; window.clearInterval(timer); };
}

export function subscribeToDriverLocation(orderId, onLocation) {
  let active = true;
  const refresh = async () => {
    try {
      const { data } = await api.get(`/orders/${orderId}/driver-location`);
      if (active) onLocation(data);
    } catch { /* no assignment/location yet; keep waiting */ }
  };
  refresh();
  const timer = window.setInterval(refresh, 5000);
  return () => { active = false; window.clearInterval(timer); };
}
