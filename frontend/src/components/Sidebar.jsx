import { NavLink, useLocation } from "react-router-dom";
import { useAuth } from "../contexts/AuthContext";
import { menuGroups, menuItems } from "../config/menuConfig";
import { Icon } from "./Icon";

export function Sidebar({ collapsed, onToggle, mobileOpen, onMobileClose }) {
  const { user, logout } = useAuth();
  const location = useLocation();

  const isSuperAdmin = user?.roles?.includes("SUPER_ADMIN");

  const visibleItems = menuItems.filter((item) => {
    if (!item.roles) return true;
    return item.roles.some((r) => user?.roles?.includes(r));
  });

  const getInitials = (nome) => {
    if (!nome) return "U";
    return nome
      .split(" ")
      .slice(0, 2)
      .map((n) => n[0])
      .join("")
      .toUpperCase();
  };

  const getRoleLabel = (roles) => {
    if (!roles || !roles.length) return "";
    if (roles.includes("SUPER_ADMIN")) return "Super Admin";
    if (roles.includes("GESTOR")) return "Gestor";
    return "Usuário";
  };

  return (
    <aside className={`sidebar ${collapsed ? "collapsed" : ""} ${mobileOpen ? "mobile-open" : ""}`}>
      {/* Logo */}
      <div className="sidebar-logo">
        <div className="sidebar-logo-icon">A</div>
        {!collapsed && (
          <span className="sidebar-logo-text">
            Alfa<span>School</span>
          </span>
        )}
      </div>

      {/* Navigation */}
      <nav className="sidebar-nav">
        {menuGroups.map((group) => {
          const groupItems = visibleItems.filter((item) => item.group === group.key);
          if (!groupItems.length) return null;

          return (
            <div key={group.key}>
              {group.label && (
                <div className="sidebar-group-label">{group.label}</div>
              )}
              {groupItems.map((item) => {
                const isActive =
                  item.path === "/"
                    ? location.pathname === "/"
                    : location.pathname.startsWith(item.path);

                return (
                  <NavLink
                    key={item.path}
                    to={item.path}
                    className={`sidebar-item ${isActive ? "active" : ""}`}
                    onClick={onMobileClose}
                    title={collapsed ? item.label : undefined}
                  >
                    <span className="sidebar-item-icon">
                      <Icon name={item.icon} size={16} />
                    </span>
                    <span className="sidebar-item-label">{item.label}</span>
                    {collapsed && (
                      <span className="tooltip">{item.label}</span>
                    )}
                  </NavLink>
                );
              })}
            </div>
          );
        })}
      </nav>

      {/* User section */}
      <div className="sidebar-user">
        <div className="sidebar-avatar">{getInitials(user?.nome)}</div>
        <div className="sidebar-user-info">
          <div className="sidebar-user-name">{user?.nome || "Usuário"}</div>
          <div className="sidebar-user-role">{getRoleLabel(user?.roles)}</div>
        </div>
        <button
          className="sidebar-logout-btn"
          onClick={logout}
          title="Sair"
        >
          <Icon name="LogOut" size={16} />
        </button>
      </div>

      {/* Collapse toggle */}
      <div className="sidebar-collapse-btn">
        <button onClick={onToggle} title={collapsed ? "Expandir" : "Recolher"}>
          <Icon name={collapsed ? "ChevronRight" : "ChevronLeft"} size={14} />
        </button>
      </div>
    </aside>
  );
}
