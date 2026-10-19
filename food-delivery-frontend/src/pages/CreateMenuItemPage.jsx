import { useEffect, useState } from "react";
import { useNavigate } from "react-router-dom";
import MainLayout from "../layouts/MainLayout";
import Input from "../components/ui/Input";
import Button from "../components/ui/Button";
import { getRestaurants } from "../api/restaurantApi";
import { getCategories } from "../api/categoryApi";
import { createMenuItem, uploadMenuItemImage } from "../api/menuApi";

const MAX_IMAGES = 5;

export default function CreateMenuItemPage() {
  const navigate = useNavigate();
  const [restaurants, setRestaurants] = useState([]);
  const [categories, setCategories] = useState([]);
  const [files, setFiles] = useState([]);
  const [error, setError] = useState("");
  const [loading, setLoading] = useState(false);
  const [form, setForm] = useState({ name: "", description: "", price: "", foodType: "VEG", available: true, restaurantId: "", categoryId: "" });

  useEffect(() => {
    Promise.all([getRestaurants(), getCategories()])
      .then(([restaurantResponse, categoryResponse]) => {
        setRestaurants(restaurantResponse.data.content);
        setCategories(categoryResponse.data.content);
      })
      .catch(() => setError("Unable to load restaurants and categories."));
  }, []);

  const updateForm = (field, value) => setForm((current) => ({ ...current, [field]: value }));
  const handleFiles = (event) => {
    const selected = Array.from(event.target.files || []);
    if (selected.length > MAX_IMAGES) return setError(`Upload at most ${MAX_IMAGES} images.`);
    if (selected.some((file) => !file.type.startsWith("image/"))) return setError("Only image files can be uploaded.");
    setError("");
    setFiles(selected);
  };

  const handleSubmit = async (event) => {
    event.preventDefault();
    setError("");
    setLoading(true);
    try {
      const { data: menuItem } = await createMenuItem({ ...form, price: Number(form.price) });
      await Promise.all(files.map((file) => uploadMenuItemImage(menuItem.id, file)));
      navigate(`/restaurants/${menuItem.restaurantId}`);
    } catch (requestError) {
      setError(requestError.response?.data?.message || "Failed to create the menu item.");
    } finally { setLoading(false); }
  };

  return <MainLayout><section className="min-h-screen bg-slate-50 py-20"><div className="mx-auto max-w-xl rounded-2xl bg-white p-8 shadow">
    <h1 className="mb-8 text-3xl font-bold">Create Menu Item</h1>
    <form onSubmit={handleSubmit} className="space-y-5">
      <Input placeholder="Menu item name" required value={form.name} onChange={(event) => updateForm("name", event.target.value)} />
      <Input placeholder="Description" required value={form.description} onChange={(event) => updateForm("description", event.target.value)} />
      <Input placeholder="Price" type="number" min="0.01" step="0.01" required value={form.price} onChange={(event) => updateForm("price", event.target.value)} />
      <select className="w-full rounded-lg border p-3" value={form.foodType} onChange={(event) => updateForm("foodType", event.target.value)}>
        <option value="VEG">Vegetarian</option><option value="NON_VEG">Non-vegetarian</option>
      </select>
      <select required className="w-full rounded-lg border p-3" value={form.restaurantId} onChange={(event) => updateForm("restaurantId", event.target.value)}>
        <option value="">Select restaurant</option>{restaurants.map((restaurant) => <option key={restaurant.id} value={restaurant.id}>{restaurant.name}</option>)}
      </select>
      <select required className="w-full rounded-lg border p-3" value={form.categoryId} onChange={(event) => updateForm("categoryId", event.target.value)}>
        <option value="">Select category</option>{categories.map((category) => <option key={category.id} value={category.id}>{category.name}</option>)}
      </select>
      <label className="flex items-center gap-2 text-sm"><input type="checkbox" checked={form.available} onChange={(event) => updateForm("available", event.target.checked)} /> Available to order</label>
      <div><label className="mb-2 block text-sm font-medium">Food images (up to {MAX_IMAGES})</label><input type="file" accept="image/*" multiple onChange={handleFiles} />
        {files.length > 0 && <div className="mt-3 grid grid-cols-3 gap-2">{files.map((file) => <img key={file.name} src={URL.createObjectURL(file)} alt="Selected food" className="h-20 w-full rounded object-cover" />)}</div>}
      </div>
      {error && <p className="text-red-500">{error}</p>}<Button type="submit" className="w-full" disabled={loading}>{loading ? "Saving..." : "Create Menu Item"}</Button>
    </form>
  </div></section></MainLayout>;
}
