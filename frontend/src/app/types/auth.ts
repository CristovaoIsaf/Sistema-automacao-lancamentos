export interface LoginRequest {

    email: string;

    password: string;

}

export interface LoginResposta {

    token: string;

    tipo: string;

    email: string;

    nome: string;

    papel: string;

}