#!/bin/bash

curl -d "@vcasp.json" -H "Content-Type: application/json" -X POST http://172.2.0.3:9000/drops/organization/insert
curl -d "@vcaw.json" -H "Content-Type: application/json" -X POST http://172.2.0.3:9000/drops/organization/insert
curl -d "@vcads.json" -H "Content-Type: application/json" -X POST http://172.2.0.3:9000/drops/organization/insert
curl -d "@bankaccount.json" -H "Content-Type: application/json" -X POST http://172.2.0.3:9000/drops/organization/addBankaccount
curl -d "@profile1.json" -H "Content-Type: application/json" -X POST http://172.2.0.3:9000/drops/rest/user/create
curl -d "@profile2.json" -H "Content-Type: application/json" -X POST http://172.2.0.3:9000/drops/rest/user/create
curl -d "@profile3.json" -H "Content-Type: application/json" -X POST http://172.2.0.3:9000/drops/rest/user/create
curl -d "@profile4.json" -H "Content-Type: application/json" -X POST http://172.2.0.3:9000/drops/rest/user/create
curl -d "@profile5.json" -H "Content-Type: application/json" -X POST http://172.2.0.3:9000/drops/rest/user/create

curl -d "@op1.json" -H "Content-Type: application/json" -X POST http://172.2.0.3:9000/drops/organization/addProfile
curl -d "@op2.json" -H "Content-Type: application/json" -X POST http://172.2.0.3:9000/drops/organization/addProfile
curl -d "@op3.json" -H "Content-Type: application/json" -X POST http://172.2.0.3:9000/drops/organization/addProfile
curl -d "@op4.json" -H "Content-Type: application/json" -X POST http://172.2.0.3:9000/drops/organization/addProfile
curl -d "@op5.json" -H "Content-Type: application/json" -X POST http://172.2.0.3:9000/drops/organization/addProfile
