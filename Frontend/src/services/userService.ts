import { api } from "./api";
import type { AuthRole } from "@/types/auth";

export interface AdminUserResponse {
  id: number;
  username: string;
  fullName: string;
  email: string;
  phoneNumber: string;
  role: AuthRole;
  isActive: boolean;
  createdAt: string;
}

export interface AdminCreateUserRequest {
  username: string;
  fullName: string;
  email: string;
  phoneNumber: string;
  password: string;
  role: AuthRole;
  isActive?: boolean;
}

export interface AdminUpdateUserRequest {
  username?: string;
  fullName?: string;
  email?: string;
  phoneNumber?: string;
  password?: string;
  role?: AuthRole;
  isActive?: boolean;
}

export const userService = {
  async getAllUsers() {
    const response = await api.get<AdminUserResponse[]>("/api/admin/users");
    return response.data;
  },

  async createUser(payload: AdminCreateUserRequest) {
    const response = await api.post<AdminUserResponse>("/api/admin/users", payload);
    return response.data;
  },

  async updateUser(userId: number, payload: AdminUpdateUserRequest) {
    const response = await api.put<AdminUserResponse>(`/api/admin/users/${userId}`, payload);
    return response.data;
  },

  async deleteUser(userId: number) {
    await api.delete(`/api/admin/users/${userId}`);
  },
};
