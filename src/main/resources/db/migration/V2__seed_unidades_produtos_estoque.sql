-- =====================================================================
-- V2 - Seed de dados base para testes (unidades, produtos e estoque).
-- Usuários são criados pelo DataSeeder (Java) porque a senha precisa de hash BCrypt.
-- =====================================================================

INSERT INTO unidades (nome, cidade, uf, ativa, criado_em) VALUES
 ('Raízes do Nordeste - Boa Viagem', 'Recife',    'PE', TRUE, CURRENT_TIMESTAMP),  -- id 1
 ('Raízes do Nordeste - Pelourinho', 'Salvador',  'BA', TRUE, CURRENT_TIMESTAMP),  -- id 2
 ('Raízes do Nordeste - Iracema',    'Fortaleza', 'CE', TRUE, CURRENT_TIMESTAMP);  -- id 3

INSERT INTO produtos (nome, descricao, categoria, preco, ativo, criado_em) VALUES
 ('Tapioca de carne de sol', 'Tapioca recheada com carne de sol e queijo coalho', 'TAPIOCA', 18.90, TRUE, CURRENT_TIMESTAMP),  -- id 1
 ('Cuscuz com ovo',          'Cuscuz nordestino com ovo e manteiga da terra',     'CUSCUZ',  12.50, TRUE, CURRENT_TIMESTAMP),  -- id 2
 ('Baião de dois',           'Arroz, feijão verde, queijo coalho e nata',         'PRATO',   29.90, TRUE, CURRENT_TIMESTAMP),  -- id 3
 ('Suco de cajá',            'Suco natural 400 ml',                               'BEBIDA',   9.00, TRUE, CURRENT_TIMESTAMP),  -- id 4
 ('Bolo de rolo',            'Fatia de bolo de rolo com goiabada',                'DOCE',     8.50, TRUE, CURRENT_TIMESTAMP);  -- id 5

-- Unidade 1 (Recife): cardápio completo. Baião de dois com só 2 unidades -> usado no teste de 409.
INSERT INTO estoques (unidade_id, produto_id, quantidade, atualizado_em) VALUES
 (1, 1, 50,  CURRENT_TIMESTAMP),
 (1, 2, 40,  CURRENT_TIMESTAMP),
 (1, 3, 2,   CURRENT_TIMESTAMP),
 (1, 4, 100, CURRENT_TIMESTAMP),
 (1, 5, 30,  CURRENT_TIMESTAMP),
-- Unidade 2 (Salvador): cardápio reduzido.
 (2, 1, 20,  CURRENT_TIMESTAMP),
 (2, 4, 50,  CURRENT_TIMESTAMP),
-- Unidade 3 (Fortaleza): cuscuz esgotado (aparece no cardápio como indisponível).
 (3, 2, 0,   CURRENT_TIMESTAMP),
 (3, 4, 15,  CURRENT_TIMESTAMP);
