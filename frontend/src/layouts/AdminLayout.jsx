import { useState, useEffect } from "react";
import { Outlet } from "react-router-dom";
import { Sidebar } from "../components/Sidebar";
import { Header } from "../components/Header";

export function AdminLayout() {
  const [collapsed, setCollapsed] = useState(() => {
    return localStorage.getItem("alfaschool-sidebar-collapsed") === "true";
  });

  const [mobileOpen, setMobileOpen] = useState(false);

  const toggleCollapse = () => {
    setCollapsed((prev) => {
      const next = !prev;
      localStorage.setItem("alfaschool-sidebar-collapsed", String(next));
      return next;
    });
  };

  const toggleMobile = () => setMobileOpen((v) => !v);
  const closeMobile   = () => setMobileOpen(false);

  // Close mobile sidebar on resize to desktop
  useEffect(() => {
    const handler = () => {
      if (window.innerWidth > 860) setMobileOpen(false);
    };
    window.addEventListener("resize", handler);
    return () => window.removeEventListener("resize", handler);
  }, []);

  return (
    <div className="app-shell">
      {/* Mobile overlay */}
      <div
        className={`mobile-overlay ${mobileOpen ? "visible" : ""}`}
        onClick={closeMobile}
      />

      <Sidebar
        collapsed={collapsed}
        onToggle={toggleCollapse}
        mobileOpen={mobileOpen}
        onMobileClose={closeMobile}
      />

      <div className={`main-area ${collapsed ? "collapsed" : ""}`}>
        <Header onMenuToggle={toggleMobile} />
        <main className="content-area">
          <Outlet />
        </main>
      </div>
    </div>
  );
}
