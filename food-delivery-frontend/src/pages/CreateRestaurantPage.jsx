import { useRef, useState } from "react";

import MainLayout from "../layouts/MainLayout";

import Input from "../components/ui/Input";
import Button from "../components/ui/Button";

import { createRestaurant, uploadRestaurantImage } from "../api/restaurantApi";

const INITIAL_FORM = {
  name: "",
  description: "",
  address: "",
  city: "",
  phoneNumber: "",
  status: "OPEN",
};

const MAX_IMAGE_SIZE = 5 * 1024 * 1024; // 5 MB

export default function CreateRestaurantPage() {
  const [form, setForm] = useState(INITIAL_FORM);
  const [errors, setErrors] = useState({});
  const [error, setError] = useState("");
  const [success, setSuccess] = useState("");
  const [loading, setLoading] = useState(false);
  const [image, setImage] = useState(null);

  const imageInputRef = useRef(null);

  // Update a form field and clear its validation error.
  const handleChange = (field, value) => {
    setForm((previousForm) => ({
      ...previousForm,
      [field]: value,
    }));

    setErrors((previousErrors) => ({
      ...previousErrors,
      [field]: "",
    }));

    setError("");
    setSuccess("");
  };

  // Validate all form fields.
  const validateForm = () => {
    const localErrors = {};

    const name = form.name.trim();
    const description = form.description.trim();
    const address = form.address.trim();
    const city = form.city.trim();
    const phoneNumber = form.phoneNumber.trim();

    // Restaurant name
    if (!name) {
      localErrors.name = "Restaurant name is required";
    } else if (name.length < 3) {
      localErrors.name = "Restaurant name must be at least 3 characters";
    } else if (name.length > 160) {
      localErrors.name = "Restaurant name must not exceed 160 characters";
    }

    // Description
    if (description.length > 1000) {
      localErrors.description = "Description must not exceed 1000 characters";
    }

    // Address
    if (!address) {
      localErrors.address = "Address is required";
    } else if (address.length > 500) {
      localErrors.address = "Address must not exceed 500 characters";
    }

    // City
    if (!city) {
      localErrors.city = "City is required";
    } else if (city.length > 100) {
      localErrors.city = "City must not exceed 100 characters";
    }

    // Phone number (optional)
    if (phoneNumber) {
      const digits = phoneNumber.replace(/\D/g, "");

      if (!/^[0-9+\-\s()]+$/.test(phoneNumber)) {
        localErrors.phoneNumber = "Enter a valid phone number";
      } else if (digits.length < 10 || digits.length > 15) {
        localErrors.phoneNumber = "Phone number must contain 10 to 15 digits";
      }
    }

    // Restaurant image (optional)
    if (image) {
      if (!image.type.startsWith("image/")) {
        localErrors.image = "Please select a valid image file";
      } else if (image.size > MAX_IMAGE_SIZE) {
        localErrors.image = "Image size must not exceed 5 MB";
      }
    }

    setErrors(localErrors);

    return Object.keys(localErrors).length === 0;
  };

  const handleImageChange = (e) => {
    const selectedImage = e.target.files?.[0] || null;

    setImage(selectedImage);

    setErrors((previousErrors) => ({
      ...previousErrors,
      image: "",
    }));

    setError("");
    setSuccess("");
  };

  const handleSubmit = async (e) => {
    e.preventDefault();

    if (loading) {
      return;
    }

    setError("");
    setSuccess("");

    if (!validateForm()) {
      return;
    }

    setLoading(true);

    let restaurantCreated = false;

    try {
      const payload = {
        ...form,
        name: form.name.trim(),
        description: form.description.trim(),
        address: form.address.trim(),
        city: form.city.trim(),
        phoneNumber: form.phoneNumber.trim(),
      };

      const { data: restaurant } = await createRestaurant(payload);

      restaurantCreated = true;

      if (image) {
        await uploadRestaurantImage(restaurant.id, image);
      }

      setSuccess(
        "Application submitted. The restaurant will appear in search after admin approval.",
      );

      setForm({ ...INITIAL_FORM });
      setImage(null);
      setErrors({});

      if (imageInputRef.current) {
        imageInputRef.current.value = "";
      }
    } catch (err) {
      console.error("Restaurant creation error:", err);

      const message =
        err.response?.data?.message || err.message || "Something went wrong.";

      if (restaurantCreated) {
        setError(
          `The restaurant was created, but the image upload may have failed. ${message}`,
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
          <h1 className="mb-8 text-3xl font-bold">Create Restaurant</h1>

          {success && (
            <p
              role="status"
              aria-live="polite"
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

          <form onSubmit={handleSubmit} className="space-y-5" noValidate>
            {/* Restaurant Name */}
            <div>
              <label
                htmlFor="restaurant-name"
                className="mb-2 block text-sm font-medium"
              >
                Restaurant Name *
              </label>

              <Input
                id="restaurant-name"
                placeholder="Restaurant Name"
                value={form.name}
                onChange={(e) => handleChange("name", e.target.value)}
                required
                aria-invalid={Boolean(errors.name)}
                aria-describedby={errors.name ? "name-error" : undefined}
              />

              {errors.name && (
                <p
                  id="name-error"
                  role="alert"
                  className="mt-1 text-sm text-red-600"
                >
                  {errors.name}
                </p>
              )}
            </div>

            {/* Description */}
            <div>
              <label
                htmlFor="restaurant-description"
                className="mb-2 block text-sm font-medium"
              >
                Description
              </label>

              <Input
                id="restaurant-description"
                placeholder="Description"
                value={form.description}
                onChange={(e) => handleChange("description", e.target.value)}
                aria-invalid={Boolean(errors.description)}
                aria-describedby={
                  errors.description ? "description-error" : undefined
                }
              />

              {errors.description && (
                <p
                  id="description-error"
                  role="alert"
                  className="mt-1 text-sm text-red-600"
                >
                  {errors.description}
                </p>
              )}
            </div>

            {/* Address */}
            <div>
              <label
                htmlFor="restaurant-address"
                className="mb-2 block text-sm font-medium"
              >
                Address *
              </label>

              <Input
                id="restaurant-address"
                placeholder="Address"
                value={form.address}
                onChange={(e) => handleChange("address", e.target.value)}
                required
                aria-invalid={Boolean(errors.address)}
                aria-describedby={errors.address ? "address-error" : undefined}
              />

              {errors.address && (
                <p
                  id="address-error"
                  role="alert"
                  className="mt-1 text-sm text-red-600"
                >
                  {errors.address}
                </p>
              )}
            </div>

            {/* City */}
            <div>
              <label
                htmlFor="restaurant-city"
                className="mb-2 block text-sm font-medium"
              >
                City *
              </label>

              <Input
                id="restaurant-city"
                placeholder="City"
                value={form.city}
                onChange={(e) => handleChange("city", e.target.value)}
                required
                aria-invalid={Boolean(errors.city)}
                aria-describedby={errors.city ? "city-error" : undefined}
              />

              {errors.city && (
                <p
                  id="city-error"
                  role="alert"
                  className="mt-1 text-sm text-red-600"
                >
                  {errors.city}
                </p>
              )}
            </div>

            {/* Phone Number */}
            <div>
              <label
                htmlFor="restaurant-phone"
                className="mb-2 block text-sm font-medium"
              >
                Phone Number
              </label>

              <Input
                id="restaurant-phone"
                type="tel"
                placeholder="Phone Number"
                value={form.phoneNumber}
                onChange={(e) => handleChange("phoneNumber", e.target.value)}
                aria-invalid={Boolean(errors.phoneNumber)}
                aria-describedby={
                  errors.phoneNumber ? "phone-error" : undefined
                }
              />

              {errors.phoneNumber && (
                <p
                  id="phone-error"
                  role="alert"
                  className="mt-1 text-sm text-red-600"
                >
                  {errors.phoneNumber}
                </p>
              )}
            </div>

            {/* Restaurant Image */}
            <div>
              <label
                htmlFor="restaurant-image"
                className="mb-2 block text-sm font-medium"
              >
                Restaurant Image (Optional, max 5 MB)
              </label>

              <input
                ref={imageInputRef}
                id="restaurant-image"
                type="file"
                accept="image/*"
                onChange={handleImageChange}
                aria-invalid={Boolean(errors.image)}
                aria-describedby={errors.image ? "image-error" : undefined}
                className="block w-full text-sm"
              />

              {image && (
                <p className="mt-1 text-sm text-slate-500">
                  Selected: {image.name}
                </p>
              )}

              {errors.image && (
                <p
                  id="image-error"
                  role="alert"
                  className="mt-1 text-sm text-red-600"
                >
                  {errors.image}
                </p>
              )}
            </div>

            {/* Submit */}
            <Button type="submit" className="w-full" disabled={loading}>
              {loading ? "Creating..." : "Create Restaurant"}
            </Button>
          </form>
        </div>
      </section>
    </MainLayout>
  );
}
