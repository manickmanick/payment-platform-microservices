import Card from "../ui/Card";
import Button from "../ui/Button";
import { Link } from "react-router-dom";
import { useEffect, useState } from "react";
import { addFavorite, getFavorites, removeFavorite } from "../../api/favoriteApi";

export default function RestaurantCard({ restaurant }) {
  const [favorite, setFavorite] = useState(false);
  const [favoriteError, setFavoriteError] = useState("");
  useEffect(() => { if (localStorage.getItem("token")) getFavorites().then(({data}) => setFavorite((data.content || []).some((x) => x.restaurantId === restaurant.id))).catch(() => {}); }, [restaurant.id]);
  const toggleFavorite = async () => {
    setFavoriteError("");
    try { if (favorite) await removeFavorite(restaurant.id); else await addFavorite(restaurant.id); setFavorite((x) => !x); }
    catch { setFavoriteError("Sign in to save favorites."); }
  };
  return (
    <Card className="overflow-hidden transition hover:scale-[1.02]">
      <Link to={`/restaurants/${restaurant.id}`}>
        <img
          src={restaurant.imageUrl || "https://images.unsplash.com/photo-1515003197210-e0cd71810b5f"}
          alt={restaurant.name}
          className="h-52 w-full object-cover"
        />
        <div className="p-5">
          <h3 className="text-2xl font-bold">{restaurant.name}</h3>

          <p className="mt-2 text-gray-500">{restaurant.description}</p>

          <p className="mt-2 text-sm text-slate-600">📍 {restaurant.address}</p>

          <p className="mt-2 font-medium">⭐ {restaurant.averageRating || "New"} ({restaurant.reviewCount || 0})</p>

          <Button className="mt-5">View Menu</Button>
        </div>
      </Link>
      <div className="flex items-center justify-between px-5 pb-4"><button type="button" aria-pressed={favorite} onClick={toggleFavorite} className="rounded-lg border px-3 py-2 text-sm">{favorite ? "♥ Saved" : "♡ Save"}</button>{favoriteError && <span role="status" className="text-xs text-red-600">{favoriteError}</span>}</div>
    </Card>
  );
}
