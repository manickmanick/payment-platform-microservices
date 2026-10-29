
import { useRef, useState } from "react";

import MainLayout from "../layouts/MainLayout";

import Input from "../components/ui/Input";
import Button from "../components/ui/Button";

import {
    createRestaurant,
    uploadRestaurantImage,
} from "../api/restaurantApi";

const INITIAL_FORM = {
    name: "",
    description: "",
    address: "",
    city: "",
    phoneNumber: "",
    status: "OPEN",
};

export default function CreateRestaurantPage() {
    const [form, setForm] = useState(INITIAL_FORM);
    const [error, setError] = useState("");
    const [loading, setLoading] = useState(false);
    const [image, setImage] = useState(null);
    const [success, setSuccess] = useState("");

    const imageInputRef = useRef(null);

    const handleChange = (field, value) => {
        setForm((previousForm) => ({
            ...previousForm,
            [field]: value,
        }));
    };

    const handleSubmit = async (e) => {
        e.preventDefault();

        if (loading) {
            return;
        }

        setLoading(true);
        setError("");
        setSuccess("");

        let restaurantCreated = false;

        try {
            const { data: restaurant } = await createRestaurant(form);
            restaurantCreated = true;

            if (image) {
                await uploadRestaurantImage(restaurant.id, image);
            }

            setSuccess(
                "Application submitted. The restaurant will appear in search after admin approval."
            );

            // Clear the form after successful submission.
            setForm({ ...INITIAL_FORM });
            setImage(null);

            // Clear the actual file input element.
            if (imageInputRef.current) {
                imageInputRef.current.value = "";
            }
        } catch (err) {
            console.error("Restaurant creation error:", err);

            const message =
                err.response?.data?.message ||
                err.message ||
                "Something went wrong.";

            if (restaurantCreated) {
                setError(
                    `The restaurant was created, but the image upload may have failed. ${message}`
                );
            } else {
                setError(message);
            }
        } finally {
            setLoading(false);
        }
    };

    return (
        <MainLayout>
            <section className="min-h-screen bg-slate-50 py-20">
                <div className="mx-auto max-w-xl rounded-2xl bg-white p-8 shadow-lg">
                    <h1 className="mb-8 text-3xl font-bold">
                        Create Restaurant
                    </h1>

                    {success && (
                        <p
                            role="status"
                            className="mb-5 rounded-lg bg-green-50 p-3 text-green-800"
                        >
                            {success}
                        </p>
                    )}

                    {error && (
                        <div
                            role="alert"
                            className="mb-5 rounded-lg border border-red-200 bg-red-50 p-3 text-red-600"
                        >
                            {error}
                        </div>
                    )}

                    <form onSubmit={handleSubmit} className="space-y-5">
                        <Input
                            placeholder="Restaurant Name"
                            value={form.name}
                            onChange={(e) =>
                                handleChange("name", e.target.value)
                            }
                            required
                        />

                        <Input
                            placeholder="Description"
                            value={form.description}
                            onChange={(e) =>
                                handleChange("description", e.target.value)
                            }
                        />

                        <Input
                            placeholder="Address"
                            value={form.address}
                            onChange={(e) =>
                                handleChange("address", e.target.value)
                            }
                            required
                        />

                        <Input
                            placeholder="City"
                            value={form.city}
                            onChange={(e) =>
                                handleChange("city", e.target.value)
                            }
                            required
                        />

                        <Input
                            placeholder="Phone Number"
                            value={form.phoneNumber}
                            onChange={(e) =>
                                handleChange("phoneNumber", e.target.value)
                            }
                        />

                        <div>
                            <label
                                htmlFor="restaurant-image"
                                className="mb-2 block text-sm font-medium"
                            >
                                Restaurant image
                            </label>

                            <input
                                ref={imageInputRef}
                                id="restaurant-image"
                                type="file"
                                accept="image/*"
                                onChange={(e) =>
                                    setImage(e.target.files?.[0] || null)
                                }
                                className="block w-full text-sm"
                            />

                            {image && (
                                <p className="mt-1 text-sm text-slate-500">
                                    Selected: {image.name}
                                </p>
                            )}
                        </div>

                        <Button
                            type="submit"
                            className="w-full"
                            disabled={loading}
                        >
                            {loading ? "Creating..." : "Create Restaurant"}
                        </Button>
                    </form>
                </div>
            </section>
        </MainLayout>
    );
}
