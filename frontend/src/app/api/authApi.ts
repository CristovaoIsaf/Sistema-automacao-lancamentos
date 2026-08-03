import { apiPost } from "./client";
import type { LoginResposta } from "../types/auth";

export async function login(
  email: string,
  password: string
): Promise<LoginResposta> {

  return apiPost<LoginResposta>(
    "/auth/login",
    {
      email,
      password
    }
  );

}