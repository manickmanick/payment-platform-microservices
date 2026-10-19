import { useEffect, useState } from "react";

import { Swiper, SwiperSlide } from "swiper/react";

import "swiper/css";

import { getCategories } from "../../api/categoryApi";

export default function CategoriesSection() {
  const [categories, setCategories] = useState([]);

  useEffect(() => {
    loadCategories();
  }, []);

  const loadCategories = async () => {
    try {
      const response = await getCategories();

      setCategories(response.data.content);
    } catch (error) {
      console.error(error);
    }
  };

  return (
    <section className="py-12">
      <div className="mx-auto max-w-7xl px-6">
        <h2 className="mb-8 text-3xl font-bold">Categories</h2>

        <Swiper
          spaceBetween={16}
          slidesPerView={2}
          breakpoints={{
            640: {
              slidesPerView: 3,
            },
            768: {
              slidesPerView: 4,
            },
            1024: {
              slidesPerView: 6,
            },
          }}
        >
          {categories.map((category) => (
            <SwiperSlide key={category.id}>
              <div className="cursor-pointer rounded-xl bg-white p-6 text-center shadow hover:shadow-lg">
                {category.name}
              </div>
            </SwiperSlide>
          ))}
        </Swiper>
      </div>
    </section>
  );
}
