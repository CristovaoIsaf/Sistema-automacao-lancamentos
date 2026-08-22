import { apiPost } from "./client";
import type { LoginResposta } from "../types/auth";

export async function login(
  email: string,
  password: string,
  codigo2FA?: string
): Promise<LoginResposta> {

  return apiPost<LoginResposta>(
    "/auth/login",
    {
      email,
      password,
      ...(codigo2FA ? { codigo2FA } : {})
    }
  );

}