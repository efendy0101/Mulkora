"use client";

import { useEffect, useState } from "react";
import { onAdminAuthStateChanged, type AdminUser } from "@/lib/auth";

export interface AdminAuthState {
  user: AdminUser | null;
  loading: boolean;
}

/**
 * Admin panel kimlik doğrulama durumu hook'u.
 * Firebase Auth durumunu ve custom claim doğrulamasını yönetir.
 */
export function useAdminAuth(): AdminAuthState {
  const [state, setState] = useState<AdminAuthState>({
    user: null,
    loading: true,
  });

  useEffect(() => {
    const unsubscribe = onAdminAuthStateChanged((user) => {
      setState({ user, loading: false });
    });
    return unsubscribe;
  }, []);

  return state;
}