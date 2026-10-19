import { useState } from "react";

import { ShoppingCart, Menu, X } from "lucide-react";

import { Link, NavLink } from "react-router-dom";

import { useDispatch, useSelector } from "react-redux";

import { logoutSuccess } from "../../redux/slices/authSlice";

export default function Navbar() {
  const dispatch = useDispatch();

  const user = useSelector((state) => state.auth.user);

  const cartCount = useSelector((state) => state.cart.count);

  const [mobileOpen, setMobileOpen] = useState(false);

  const navLinkClass = ({ isActive }) =>
    isActive
      ? "border-b-2 border-orange-500 pb-1 font-semibold text-orange-500 transition"
      : "font-medium text-slate-700 transition hover:text-orange-500";

  const closeMobileMenu = () => {
    setMobileOpen(false);
  };

  return (
    <header className="sticky top-0 z-50 bg-white shadow-sm">
      <div className="mx-auto flex h-20 max-w-7xl items-center justify-between px-6">
        {/* Logo */}

        <Link to="/" className="text-3xl font-extrabold text-orange-500">
          Foodie
        </Link>

        {/* Desktop Navigation */}

        <nav className="hidden xl:flex items-center gap-8">
          <NavLink to="/" end className={navLinkClass}>
            Home
          </NavLink>

          <NavLink to="/restaurants" end className={navLinkClass}>
            Restaurants
          </NavLink>

          {user?.role === "RESTAURANT_OWNER" && (
            <NavLink to="/owner/orders" end className={navLinkClass}>
              Manage Orders
            </NavLink>
          )}
          {user?.role === "ADMIN" && <NavLink to="/admin" end className={navLinkClass}>Admin</NavLink>}

          {user?.role === "DRIVER" && (
            <NavLink to="/driver" end className={navLinkClass}>Driver Dashboard</NavLink>
          )}

          {user &&
            (user.role === "ADMIN" || user.role === "RESTAURANT_OWNER") && (
              <NavLink to="/restaurants/create" end className={navLinkClass}>
                Add Restaurant
              </NavLink>
            )}

          {user?.role === "RESTAURANT_OWNER" && (
            <NavLink to="/menu/create" end className={navLinkClass}>
              Add Menu
            </NavLink>
          )}

          {user && (
            <NavLink to="/orders" end className={navLinkClass}>
              Orders
            </NavLink>
          )}

          {user && (
            <NavLink to="/addresses" end className={navLinkClass}>
              Addresses
            </NavLink>
          )}

          {user && <NavLink to="/notifications" end className={navLinkClass}>Notifications</NavLink>}

          {user && (
            <NavLink
              to="/cart"
              end
              className={({ isActive }) =>
                isActive
                  ? "flex items-center gap-2 border-b-2 border-orange-500 pb-1 font-semibold text-orange-500"
                  : "flex items-center gap-2 font-medium text-slate-700 hover:text-orange-500"
              }
            >
              <ShoppingCart size={18} />
              Cart
              <span className="rounded-full bg-orange-500 px-2 py-1 text-xs text-white">
                {cartCount}
              </span>
            </NavLink>
          )}

          {user ? (
            <button
              onClick={() => dispatch(logoutSuccess())}
              className="font-medium text-slate-700 hover:text-orange-500"
            >
              Logout
            </button>
          ) : (
            <NavLink to="/login" className={navLinkClass} end>
              Login
            </NavLink>
          )}
        </nav>

        {/* Mobile Menu Button */}

        <button
          className="xl:hidden"
          onClick={() => setMobileOpen(!mobileOpen)}
        >
          {mobileOpen ? <X size={28} /> : <Menu size={28} />}
        </button>
      </div>

      {/* Mobile Menu */}

      {mobileOpen && (
        <div className="border-t bg-white xl:hidden">
          <div className="flex flex-col p-4">
            <NavLink
              to="/"
              end
              className={({ isActive }) =>
                `${navLinkClass({
                  isActive,
                })} py-3`
              }
              onClick={closeMobileMenu}
            >
              Home
            </NavLink>

            <NavLink
              to="/restaurants"
              end
              className={({ isActive }) =>
                `${navLinkClass({
                  isActive,
                })} py-3`
              }
              onClick={closeMobileMenu}
            >
              Restaurants
            </NavLink>

            {user?.role === "RESTAURANT_OWNER" && (
              <NavLink
                end
                to="/owner/orders"
                className={({ isActive }) =>
                  `${navLinkClass({
                    isActive,
                  })} py-3`
                }
                onClick={closeMobileMenu}
              >
                Manage Orders
              </NavLink>
            )}

            {user?.role === "DRIVER" && (
              <NavLink end to="/driver" className={({ isActive }) => `${navLinkClass({ isActive })} py-3`} onClick={closeMobileMenu}>Driver Dashboard</NavLink>
            )}

            {user &&
              (user.role === "ADMIN" || user.role === "RESTAURANT_OWNER") && (
                <NavLink
                  end
                  to="/restaurants/create"
                  className={({ isActive }) =>
                    `${navLinkClass({
                      isActive,
                    })} py-3`
                  }
                  onClick={closeMobileMenu}
                >
                  Add Restaurant
                </NavLink>
              )}

            {user?.role === "RESTAURANT_OWNER" && (
              <NavLink
                end
                to="/menu/create"
                className={({ isActive }) =>
                  `${navLinkClass({
                    isActive,
                  })} py-3`
                }
                onClick={closeMobileMenu}
              >
                Add Menu
              </NavLink>
            )}

            {user && (
              <NavLink
                end
                to="/orders"
                className={({ isActive }) =>
                  `${navLinkClass({
                    isActive,
                  })} py-3`
                }
                onClick={closeMobileMenu}
              >
                Orders
              </NavLink>
            )}

            {user && (
              <NavLink end to="/notifications" className={({ isActive }) => `${navLinkClass({ isActive })} py-3`} onClick={closeMobileMenu}>Notifications</NavLink>
            )}

            {user && (
              <NavLink
                end
                to="/addresses"
                className={({ isActive }) =>
                  `${navLinkClass({
                    isActive,
                  })} py-3`
                }
                onClick={closeMobileMenu}
              >
                Addresses
              </NavLink>
            )}

            {user && (
              <NavLink
                end
                to="/cart"
                className={({ isActive }) =>
                  `${navLinkClass({
                    isActive,
                  })} py-3`
                }
                onClick={closeMobileMenu}
              >
                Cart ({cartCount})
              </NavLink>
            )}

            {user ? (
              <button
                className="py-3 text-left font-medium text-slate-700 hover:text-orange-500"
                onClick={() => {
                  dispatch(logoutSuccess());

                  closeMobileMenu();
                }}
              >
                Logout
              </button>
            ) : (
              <NavLink
                end
                to="/login"
                className={({ isActive }) =>
                  `${navLinkClass({
                    isActive,
                  })} py-3`
                }
                onClick={closeMobileMenu}
              >
                Login
              </NavLink>
            )}
          </div>
        </div>
      )}
    </header>
  );
}
