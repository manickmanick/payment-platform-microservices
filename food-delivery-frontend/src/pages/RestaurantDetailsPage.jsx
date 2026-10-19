import { useCallback, useEffect, useState } from "react";
import { useParams } from "react-router-dom";
import MainLayout from "../layouts/MainLayout";
import { addRestaurantReview, getRestaurant, getRestaurantReviews } from "../api/restaurantApi";
import { searchMenu } from "../api/menuApi";
import MenuItemCard from "../components/menu/MenuItemCard";

export default function RestaurantDetailsPage() {
  const { id } = useParams();
  const [restaurant, setRestaurant] = useState(null);
  const [menuItems, setMenuItems] = useState([]);
  const [reviews, setReviews] = useState([]);
  const [rating, setRating] = useState(5);
  const [comment, setComment] = useState("");
  const [reviewMessage, setReviewMessage] = useState("");
  const [q, setQ] = useState("");
  const [foodType, setFoodType] = useState("");
  const [maxPrice, setMaxPrice] = useState("");
  const [error, setError] = useState("");
  const [loading, setLoading] = useState(true);
  const loadData = useCallback(async () => {
    try {
      const [r, menu, reviewsResponse] = await Promise.all([
        getRestaurant(id),
        searchMenu({ restaurantId: id, q: q || undefined, foodType: foodType || undefined, maxPrice: maxPrice || undefined, size: 100 }),
        getRestaurantReviews(id),
      ]);
      setRestaurant(r.data);
      setMenuItems(menu.data.content || []);
      setReviews(reviewsResponse.data.content || []);
      setError("");
    } catch { setError("Failed to load restaurant information."); }
    finally { setLoading(false); }
  }, [id, q, foodType, maxPrice]);
  useEffect(() => { loadData(); }, [loadData]);
  const submitReview = async (event) => {
    event.preventDefault(); setReviewMessage("");
    try { await addRestaurantReview(id, { rating: Number(rating), comment }); setComment(""); setReviewMessage("Review submitted."); await loadData(); }
    catch (e) { setReviewMessage(e.response?.data?.message || "Please sign in and try again. One review per account is allowed."); }
  };
  if (loading) return <MainLayout><div className="p-10 text-center">Loading...</div></MainLayout>;
  if (error) return <MainLayout><div className="p-10 text-center text-red-500">{error}</div></MainLayout>;
  return <MainLayout><section className="min-h-screen bg-slate-50">
    <img src={restaurant.imageUrl || "https://images.unsplash.com/photo-1515003197210-e0cd71810b5f"} alt={restaurant.name} className="h-80 w-full object-cover" />
    <div className="mx-auto max-w-7xl px-6 py-10">
      <h1 className="text-5xl font-bold">{restaurant.name}</h1><p className="mt-4 text-lg text-slate-600">{restaurant.description}</p><p className="mt-4 text-lg">📍 {restaurant.address}</p>
      <p className="mt-2 text-lg">⭐ {restaurant.averageRating || "New"} ({restaurant.reviewCount || 0} reviews)</p>
      <h2 className="mt-12 mb-5 text-3xl font-bold">Menu</h2>
      <div className="mb-6 grid gap-3 sm:grid-cols-3"><input aria-label="Search menu" value={q} onChange={(e)=>setQ(e.target.value)} placeholder="Search dishes..." className="rounded-lg border px-3 py-2"/><select aria-label="Food type" value={foodType} onChange={(e)=>setFoodType(e.target.value)} className="rounded-lg border bg-white px-3 py-2"><option value="">All food types</option><option value="VEG">Vegetarian</option><option value="NON_VEG">Non-vegetarian</option></select><input aria-label="Maximum price" type="number" min="0" value={maxPrice} onChange={(e)=>setMaxPrice(e.target.value)} placeholder="Maximum price" className="rounded-lg border px-3 py-2"/></div>
      {menuItems.length === 0 ? <div className="rounded-xl bg-white p-6 text-center shadow">No menu items match these filters.</div> : <div className="space-y-5">{menuItems.map((item)=><MenuItemCard key={item.id} item={item}/>)}</div>}
      <section className="mt-14"><h2 className="mb-5 text-3xl font-bold">Customer reviews</h2><div className="space-y-3">{reviews.map((review)=><article key={review.id} className="rounded-xl bg-white p-4 shadow-sm"><p className="font-semibold">⭐ {review.rating}/5</p><p className="mt-1 text-slate-700">{review.comment || "No written comment"}</p><time className="text-xs text-slate-500">{review.createdAt ? new Date(review.createdAt).toLocaleDateString() : ""}</time></article>)}</div>
        <form onSubmit={submitReview} className="mt-6 max-w-xl space-y-3 rounded-xl bg-white p-5 shadow-sm"><label className="block font-medium" htmlFor="review-rating">Your rating</label><select id="review-rating" value={rating} onChange={(e)=>setRating(e.target.value)} className="rounded border px-3 py-2"><option value="5">5 — Excellent</option><option value="4">4 — Good</option><option value="3">3 — Okay</option><option value="2">2 — Poor</option><option value="1">1 — Bad</option></select><textarea value={comment} onChange={(e)=>setComment(e.target.value)} maxLength={1000} placeholder="Share your experience" className="block w-full rounded border p-3"/><button className="rounded-lg bg-orange-600 px-4 py-2 font-semibold text-white">Submit review</button>{reviewMessage&&<p role="status" className="text-sm text-slate-600">{reviewMessage}</p>}</form>
      </section>
    </div>
  </section></MainLayout>;
}
