export interface LoginRequest {

    email: string;

    password: string;

    codigo2FA?: string;

}

export interface LoginResposta {

    id: number;

    token: string;

    tipo: string;

    email: string;

    nome: string;

    papel: string;

    requiresTwoFactor: boolean;

}