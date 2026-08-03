const API_BASE =
  (import.meta as any).env?.VITE_API_BASE_URL ??
  'http://localhost:8080';


let tokenEmMemoria: string | null = null;


export function setToken(token: string | null) {
  tokenEmMemoria = token;
}


export function getToken(): string | null {
  return tokenEmMemoria;
}


function headers() {

  return {
    'Content-Type': 'application/json',

    ...(tokenEmMemoria
      ? {
          Authorization:
            `Bearer ${tokenEmMemoria}`
        }
      : {})
  };

}


export async function apiGet<T>(
  path: string
): Promise<T> {

  const response = await fetch(
    `${API_BASE}${path}`,
    {
      method: 'GET',
      headers: headers()
    }
  );


  if (!response.ok) {
    throw new Error(
      `Erro ${response.status}`
    );
  }


  return response.json();
}



export async function apiPost<T>(
  path: string,
  body: unknown
): Promise<T> {

  const response = await fetch(
    `${API_BASE}${path}`,
    {
      method: 'POST',
      headers: headers(),
      body: JSON.stringify(body)
    }
  );


  if (!response.ok) {
    throw new Error(
      `Erro ${response.status}`
    );
  }


  return response.json();
}



export async function apiGetBlob(
  path: string
): Promise<Blob> {

  const response = await fetch(
    `${API_BASE}${path}`,
    {
      method: 'GET',
      headers: headers()
    }
  );


  if (!response.ok) {
    throw new Error(
      `Erro ${response.status}`
    );
  }


  return response.blob();
}



export async function apiPut<T>(
  path: string,
  body: unknown
): Promise<T> {

  const response = await fetch(
    `${API_BASE}${path}`,
    {
      method: 'PUT',
      headers: headers(),
      body: JSON.stringify(body)
    }
  );


  if (!response.ok) {
    throw new Error(
      `Erro ${response.status}`
    );
  }


  return response.json();
}



export async function apiDelete(
  path: string
): Promise<void> {

  const response = await fetch(
    `${API_BASE}${path}`,
    {
      method: 'DELETE',
      headers: headers()
    }
  );


  if (!response.ok) {
    throw new Error(
      `Erro ${response.status}`
    );
  }
}