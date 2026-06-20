// IUserService.aidl
package com.exemplo.fluidez;

interface IUserService {
    void destroy() = 16777114; // metodo de destruicao exigido pelo Shizuku
    void exit() = 1;
    String exec(String command) = 2; // roda um comando shell e devolve a saida
}
