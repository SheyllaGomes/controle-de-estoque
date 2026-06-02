# Controle de Estoque Web

Versao web estatica do aplicativo de controle de estoque.

## Como abrir

Abra `web/index.html` no navegador ou publique a pasta `web/` em qualquer hospedagem estatica.

Login inicial:

- Usuario: `admin`
- Senha: `admin`

## Banco de dados virtual

Os dados ficam salvos no `localStorage` do navegador com a chave `controle-estoque-web-db`.
Isso permite usar a aplicacao sem servidor e sem instalacao de banco de dados.

Como o armazenamento e local ao navegador, cada computador ou navegador tera sua propria base.

## Recursos

- Login com usuarios ativos.
- Cadastro e controle de usuarios.
- Cadastro de categorias.
- Cadastro, filtro, validacao de codigo duplicado e exclusao de produtos.
- Geracao automatica de codigos para produtos e usuarios.
- Registro de entradas e saidas.
- Dashboard com graficos.
- Relatorios imprimiveis de usuarios, produtos, movimentacoes, entradas e saidas.
