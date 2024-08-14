# byzcast-tcc

TODO

## Execução local

O script de execução local se encontra em `scripts/local_exec.py`. Ele deve ser executado
via Python 3. As variáveis iniciais dele definem a topologia. Ele requer que o projeto já
tenha sido compilado com `mvn clean install`. Feito isso, após a execução dos scripts, os
logs estarão disponíveis em `scripts/lexec`. A execução do cliente deve ser feita a parte.

```sh
java -jar target/byzcast-tcc-1.0-SNAPSHOT-jar-with-dependencies.jar --groups-configs scripts/lexec --topology scripts/lexec/topology.json client
```

Os PIDs de todos os processos Java são salvos em `scripts/lexec/pids`. Isso pode ser usado
para finalizar os processos.

OBS: o script de execução local usa portas a partir da 40000, aumentando de 10 em 10.
