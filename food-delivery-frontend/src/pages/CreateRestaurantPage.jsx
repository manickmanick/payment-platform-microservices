import { useState } from "react";

import MainLayout from "../layouts/MainLayout";

import Input from "../components/ui/Input";
import Button from "../components/ui/Button";

import { createRestaurant, uploadRestaurantImage } from "../api/restaurantApi";

export default function CreateRestaurantPage() {

  const [form, setForm] = useState({
    name: "",
    description: "",
    address: "",
    city: "",
    phoneNumber: "",
    status: "OPEN",
  });

  const [error, setError] = useState("");

  const [loading, setLoading] = useState(false);
  const [image, setImage] = useState(null);
  const [success, setSuccess] = useState("");

  const handleSubmit = async (e) => {
    e.preventDefault();

    try {
      setLoading(true);

      const { data: restaurant } = await createRestaurant(form);
      if (image) await uploadRestaurantImage(restaurant.id, image);
      setSuccess("Application submitted. The restaurant will appear in search after admin approval.");
    } catch (error) {
      setError(error.response?.data?.message || "Something went wrong");
    } finally {
      setLoading(false);
    }
  };

  return (
    <MainLayout>
      <section className="min-h-screen bg-slate-50 py-20">
        <div className="mx-auto max-w-xl rounded-2xl bg-white p-8 shadow-lg">
          <h1 className="mb-8 text-3xl font-bold">Create Restaurant</h1>
          {success && <p role="status" className="mb-5 rounded-lg bg-green-50 p-3 text-green-800">{success}</p>}

          <form onSubmit={handleSubmit} className="space-y-5">
            <Input
              placeholder="Restaurant Name"
              value={form.name}
              onChange={(e) =>
                setForm({
                  ...form,
                  name: e.target.value,
                })
              }
            />

            <Input
              placeholder="Description"
              value={form.description}
              onChange={(e) =>
                setForm({
                  ...form,
                  description: e.target.value,
                })
              }
            />

            <Input
              placeholder="Address"
              value={form.address}
              onChange={(e) =>
                setForm({
                  ...form,
                  address: e.target.value,
                })
              }
            />

            <Input
              placeholder="City"
              value={form.city}
              onChange={(e) =>
                setForm({
                  ...form,
                  city: e.target.value,
                })
              }
            />

            <Input placeholder="Phone Number" value={form.phoneNumber} onChange={(e) => setForm({ ...form, phoneNumber: e.target.value })} />

            <div>
              <label className="mb-2 block text-sm font-medium">Restaurant image</label>
              <input type="file" accept="image/*" onChange={(e) => setImage(e.target.files?.[0] || null)} />
              {image && <p className="mt-1 text-sm text-slate-500">{image.name}</p>}
            </div>

            {error && (
              <div className="rounded-lg border border-red-200 bg-red-50 p-3 text-red-600">
                {error}
              </div>
            )}

            <Button type="submit" className="w-full">
              {loading ? "Creating..." : "Create Restaurant"}
            </Button>
          </form>
        </div>
      </section>
    </MainLayout>
  );
}
