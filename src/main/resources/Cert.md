```shell
# 生成CA证书
openssl genrsa -aes256  -passout pass:seakeer -out SeakeerCaPriKey_PEM.key 2048
openssl req -new -x509 -passin pass:seakeer -key SeakeerCaPriKey_PEM.key -out SeakeerCaCert_PEM.pem -days 365000

# 签发服务端证书
openssl genrsa -aes256  -passout pass:seakeer -out ServerPriKey_PEM.key 2048  
openssl req -new -key ServerPriKey_PEM.key -out Server.csr
openssl x509 -req -in Server.csr -CA SeakeerCaCert_PEM.pem -CAkey SeakeerCaPriKey_PEM.key -CAcreateserial -out Server.crt -days 365000 -sha256 -passin pass:seakeer
openssl pkcs12 -export -out Server.p12 -inkey ServerPriKey_PEM.key -in Server.crt -certfile SeakeerCaCert_PEM.pem -passout pass:seakeer

# 签发客户端证书
openssl genrsa -aes256  -passout pass:seakeer -out ClientPriKey_PEM.key 2048
openssl req -new -key ClientPriKey_PEM.key -out Client.csr
openssl x509 -req -in Client.csr -CA SeakeerCaCert_PEM.pem -CAkey SeakeerCaPriKey_PEM.key -CAcreateserial -out Client.crt -days 365000 -sha256 -passin pass:seakeer
openssl pkcs12 -export -out Client.p12 -inkey ClientPriKey_PEM.key -in Client.crt -certfile SeakeerCaCert_PEM.pem -passout pass:seakeer


### JDK8 对于p12证书的支持可能有问题，转为jks格式
keytool -importkeystore -srckeystore Server.p12 -destkeystore Server.jks -deststoretype JKS
keytool -importkeystore -srckeystore Client.p12 -destkeystore Client.jks -deststoretype JKS
```

