import { useEffect, useState } from "react";
import MainLayout from "../layouts/MainLayout";
import Input from "../components/ui/Input";
import RestaurantCard from "../components/restaurant/RestaurantCard";
import { getRestaurants } from "../api/restaurantApi";

export default function RestaurantsPage() {
  const [restaurants, setRestaurants] = useState([]);
  const [search, setSearch] = useState("");
  const [city, setCity] = useState("");
  const [status, setStatus] = useState("OPEN");
  const [page, setPage] = useState(0);
  const [pageCount, setPageCount] = useState(0);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");

  useEffect(() => {
    let active = true;
    setLoading(true);
    getRestaurants({ q: search || undefined, city: city || undefined, status: status || undefined, page, size: 12 })
      .then(({ data }) => { if (active) { setRestaurants(data.content || []); setPageCount(data.totalPages || 0); setError(""); } })
      .catch(() => { if (active) setError("Failed to load restaurants"); })
      .finally(() => { if (active) setLoading(false); });
    return () => { active = false; };
  }, [search, city, status, page]);

  return <MainLayout><section className="min-h-screen bg-slate-50 py-16"><div className="mx-auto max-w-7xl px-6">
    <h1 className="mb-10 text-center text-5xl font-bold">Restaurants</h1>
    <div className="mx-auto mb-10 grid max-w-4xl gap-3 md:grid-cols-3">
      <Input placeholder="Search restaurants..." value={search} onChange={(e) => { setSearch(e.target.value); setPage(0); }} />
      <Input placeholder="Filter by city" value={city} onChange={(e) => { setCity(e.target.value); setPage(0); }} />
      <select aria-label="Restaurant availability" value={status} onChange={(e) => { setStatus(e.target.value); setPage(0); }} className="rounded-lg border border-slate-300 bg-white px-3 py-2"><option value="OPEN">Open now</option><option value="">All restaurants</option><option value="CLOSED">Closed</option></select>
    </div>
    {loading && <p className="text-center">Loading restaurants...</p>}{error && <p role="alert" className="text-center text-red-500">{error}</p>}
    {!loading && !error && restaurants.length === 0 && <p className="text-center text-slate-500">No restaurants match these filters.</p>}
    {!loading && <div className="grid gap-8 md:grid-cols-2 lg:grid-cols-3">{restaurants.map((restaurant) => <RestaurantCard key={restaurant.id} restaurant={restaurant} />)}</div>}
    <div className="mt-8 flex items-center justify-center gap-4"><button className="rounded border bg-white px-4 py-2 disabled:opacity-50" disabled={page===0} onClick={()=>setPage((p)=>p-1)}>Previous</button><span>Page {page+1} of {Math.max(1,pageCount)}</span><button className="rounded border bg-white px-4 py-2 disabled:opacity-50" disabled={page+1>=pageCount} onClick={()=>setPage((p)=>p+1)}>Next</button></div>
  </div></section></MainLayout>;
}
