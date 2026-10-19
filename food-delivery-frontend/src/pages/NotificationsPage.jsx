import { useEffect, useState } from "react";
import { useSelector } from "react-redux";
import MainLayout from "../layouts/MainLayout";
import { getNotifications, markAllNotificationsRead, markNotificationRead } from "../api/notificationApi";
import { subscribeToNotifications } from "../socket";

export default function NotificationsPage() {
  const user = useSelector((state) => state.auth.user);
  const [notifications, setNotifications] = useState([]);
  const [error, setError] = useState("");
  const load = () => getNotifications().then((response) => setNotifications(response.data.content)).catch(() => setError("Unable to load notifications."));
  useEffect(() => {
    load();
    return subscribeToNotifications(user.id, (notification) => setNotifications((current) => [notification, ...current]));
  }, [user.id]);
  const markRead = async (id) => { await markNotificationRead(id); setNotifications((rows) => rows.map((item) => item.id === id ? { ...item, read: true } : item)); };
  const markAllRead = async () => { await markAllNotificationsRead(); setNotifications((rows) => rows.map((item) => ({ ...item, read: true }))); };
  return <MainLayout><section className="mx-auto min-h-screen max-w-4xl px-6 py-12"><h1 className="mb-8 text-4xl font-bold">Notifications</h1>
    {notifications.some((item) => !item.read) && <button onClick={markAllRead} className="mb-5 rounded-lg border bg-white px-4 py-2 text-sm">Mark all as read</button>}
    {error && <p className="text-red-600">{error}</p>}{notifications.length === 0 ? <div className="rounded-xl bg-white p-6 shadow">No notifications yet.</div> :
      <div className="space-y-3">{notifications.map((notification) => <article key={notification.id} className={`rounded-xl bg-white p-5 shadow ${notification.read ? "opacity-70" : "border-l-4 border-orange-500"}`}><div className="flex justify-between gap-4"><h2 className="font-bold">{notification.title}</h2><span className="text-xs text-slate-500">{new Date(notification.createdAt).toLocaleString()}</span></div><p className="mt-2 text-slate-600">{notification.message}</p>{!notification.read&&<button onClick={()=>markRead(notification.id)} className="mt-3 text-sm font-semibold text-orange-700">Mark as read</button>}</article>)}</div>}
  </section></MainLayout>;
}
