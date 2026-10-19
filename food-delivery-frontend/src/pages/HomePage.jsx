import MainLayout from "../layouts/MainLayout";
import HeroSection from "../components/home/HeroSection";
import CategoriesSection from "../components/home/CategoriesSection";
import RestaurantSection from "../components/home/RestaurantSection";

export default function HomePage() {
  return (
    <MainLayout>
      <HeroSection />
      {/* <CategoriesSection /> */}
      <RestaurantSection />
    </MainLayout>
  );
}