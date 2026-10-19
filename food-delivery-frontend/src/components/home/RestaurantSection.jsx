import { useEffect, useState } from "react";

import { Swiper, SwiperSlide } from "swiper/react";

import { Navigation, Autoplay } from "swiper/modules";

import "swiper/css";
import "swiper/css/navigation";

import { getRestaurants } from "../../api/restaurantApi";

import RestaurantCard from "../restaurant/RestaurantCard";

export default function RestaurantSection() {
  const [restaurants, setRestaurants] = useState([]);

  useEffect(() => {
    loadRestaurants();
  }, []);

  const loadRestaurants = async () => {
    try {
      const response = await getRestaurants();
      console.log("response -> ",response.data);
      
      setRestaurants(response.data.content);
    } catch (error) {
      console.error(error);
    }
  };

  if (restaurants.length === 0) {
    return (
      <section className="py-12">
        <div className="mx-auto max-w-7xl px-6">
          <h2 className="mb-8 text-3xl font-bold">Popular Restaurants</h2>

          <p>No restaurants found.</p>
        </div>
      </section>
    );
  }

  return (
    <section className="py-12">
      <div className="mx-auto max-w-7xl px-6">
        <h2 className="mb-8 text-3xl font-bold">Popular Restaurants</h2>

        <Swiper
          modules={[Navigation, Autoplay]}
          navigation={true}
          autoplay={{
            delay: 3000,
            disableOnInteraction: false,
          }}
          spaceBetween={20}
          slidesPerView={1}
          breakpoints={{
            640: {
              slidesPerView: 1,
            },

            768: {
              slidesPerView: 2,
            },

            1280: {
              slidesPerView: 3,
            },
          }}
        >
          {restaurants.map((restaurant) => (
            <SwiperSlide key={restaurant.id}>
              <RestaurantCard restaurant={restaurant} />
            </SwiperSlide>
          ))}
        </Swiper>
      </div>
    </section>
  );
}
