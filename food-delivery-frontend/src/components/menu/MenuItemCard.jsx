import Card from "../ui/Card";
import Button from "../ui/Button";

import { addToCart } from "../../api/cartApi";
import { useDispatch, useSelector } from "react-redux";

import { loadCart } from "../../utils/loadCart";

export default function MenuItemCard({ item }) {
  const dispatch = useDispatch();
  const user = useSelector((state) => state.auth.user);

  const handleAddToCart = async () => {
    try {
      const response = await addToCart(item.id, 1);

      await loadCart(dispatch);

     
    } catch (error) {
      alert(error.response?.data?.message);
    }
  };

  return (
    <Card className="overflow-hidden">
      <div className="flex flex-col md:flex-row">
        <img src=""
          // src={
            // item.imageUrls?.[0] ||
            // "https://images.unsplash.com/photo-1546069901-ba9599a7e63c"
          // }
          alt={item.name}
          className="h-48 w-full object-cover md:w-64"
        />

        <div className="flex flex-1 flex-col justify-between p-5">
          <div>
            <h3 className="text-2xl font-bold">{item.name}</h3>

            <p className="mt-3 text-gray-500">{item.description}</p>

            <p className="mt-4 font-semibold">₹{item.price}</p>

            {item.category && (
              <p className="mt-2 text-sm text-orange-500">
                {item.category.name}
              </p>
            )}
          </div>

          <Button
            className="mt-5 w-fit"
            onClick={handleAddToCart}
            disabled={!user}
          >
            Add To Cart
          </Button>
        </div>
      </div>
    </Card>
  );
}
