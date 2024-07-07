# zeuscommerce
# make sure mongoDB has replica set (is needed to for transiactions)
Clone other 2 repo :
- https://github.com/engineerscodes/KhonsuDelivery
- https://github.com/engineerscodes/HathorConsumer
in Docker compose pass -> MongoDB url and RabbitMQ ->run first zeuscommerce ->HathorConsumer->KhonsuDelivery (queue to be created)
