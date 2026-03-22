import { NavLink, useLocation } from "react-router-dom";
import { useAuth } from "../contexts/AuthContext";
import { menuGroups, menuItems } from "../config/menuConfig";
import { Icon } from "./Icon";

export function Sidebar({ collapsed, onToggle, mobileOpen, onMobileClose }) {
  const { user } = useAuth();
  const location = useLocation();

  const visibleItems = menuItems.filter((item) => {
    if (!item.roles) return true;
    return item.roles.some((r) => user?.roles?.includes(r));
  });

  const groupedItems = menuGroups
    .map((group) => ({
      ...group,
      items: visibleItems.filter((item) => item.group === group.key),
    }))
    .filter((group) => group.items.length > 0);

  return (
    <aside className={`sidebar ${collapsed ? "sidebar-collapsed" : ""} ${mobileOpen ? "sidebar-mobile-open" : ""}`}>
      <div className="sidebar-top">
        <div className="sidebar-brand-wrap">
          {collapsed ? (
            <div className="sidebar-brand-icon">A</div>
          ) : (
            <span className="sidebar-brand-text">
              Alfa<span>School</span>
            </span>
          )}
        </div>
        <button
          className="sidebar-collapse-toggle"
          onClick={onToggle}
          title={collapsed ? "Expandir" : "Recolher"}
        >
          <Icon name={collapsed ? "ChevronRight" : "ChevronLeft"} size={14} />
        </button>
      </div>

      <nav className="sidebar-nav">
        {groupedItems.map((group) => (
          <div className="sidebar-group" key={group.key}>
            {group.label && (
              <div className="sidebar-group-header">
                <span className="sidebar-group-label">{group.label}</span>
              </div>
            )}
            <div className="sidebar-group-items">
              {group.items.map((item) => {
                const isActive =
                  item.path === "/"
                    ? location.pathname === "/"
                    : location.pathname.startsWith(item.path);

                return (
                  <NavLink
                    key={item.path}
                    to={item.path}
                    className={`sidebar-nav-item ${isActive ? "active" : ""}`}
                    onClick={onMobileClose}
                  >
                    <Icon name={item.icon} size={20} className="sidebar-nav-icon" />
                    <span className="sidebar-nav-label">{item.label}</span>
                    {collapsed && (
                      <span className="sidebar-tooltip">{item.label}</span>
                    )}
                  </NavLink>
                );
              })}
            </div>
          </div>
        ))}
      </nav>

      <div className="sidebar-footer">
        <p className="sidebar-footer-text">AlfaSchool v1.0</p>
      </div>
    </aside>
  );
}
