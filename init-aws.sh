#!/bin/bash
echo "Criando filas SQS no LocalStack..."

awslocal sqs create-queue --queue-name orcamento-respondido
awslocal sqs create-queue --queue-name pagamento-confirmado
awslocal sqs create-queue --queue-name execucao-resultados
awslocal sqs create-queue --queue-name gerar-orcamento
awslocal sqs create-queue --queue-name processar-pagamento
awslocal sqs create-queue --queue-name execucao-comandos

echo "Filas SQS criadas com sucesso!"