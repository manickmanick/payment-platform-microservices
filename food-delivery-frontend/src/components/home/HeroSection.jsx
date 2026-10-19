import { useNavigate } from "react-router-dom";

export default function HeroSection() {
  const navigate = useNavigate();
  return (
    <section className="bg-gradient-to-r from-orange-500 via-orange-600 to-red-500">
      <div className="mx-auto max-w-7xl px-6 py-24 md:py-32">
        <div className="max-w-3xl">
          <span className="rounded-full bg-white/20 px-4 py-2 text-white">
            Fast Delivery • Fresh Food
          </span>

          <h1 className="mt-6 text-5xl font-extrabold leading-tight text-white md:text-7xl">
            Order Food
            <br />
            Anytime Anywhere
          </h1>

          <p className="mt-6 text-lg text-orange-100 md:text-xl">
            Discover the best restaurants near you and get delicious meals
            delivered straight to your door.
          </p>

          <div className="mt-10">
            <button
              onClick={() => navigate("/restaurants")}
              className="rounded-xl bg-white px-8 py-4 text-lg font-semibold text-orange-600 shadow-lg transition hover:scale-105"
            >
              Explore Restaurants
            </button>
          </div>
        </div>
      </div>
    </section>
  );
}
