'use strict'

const http = require('http')
const fs = require('fs');
const conf = require('./config.js')
const express = require('express')
const app = express()

const JavaApp = require('./javaApp.js')

let java = bridge((msg) => {
   console.log('MESSAGE RECEIVED')
   console.log('['+msg.toString()+']')
})


process.stdin.on('readable', () => {
   let msg = process.stdin.read()
   java.status()
   let cmd = {
      action: 'add',
      category: 'users',
      details: {
         name: 'brady',
         age: 21,
         year: 'junior'
      }
   }
   console.log('sending: ' + JSON.stringify(cmd))
   java.send(JSON.stringify(cmd))
})

app.get('/', (req, res) => {
   console.log(req.url)
   fs.readFile('web/index.html', (err, data) => {
      if (err) console.error(err)
      else {
         res.writeHeader(200, {'ContentType': 'text/html'});
         res.end(data) 
      }
   })
})

app.listen(8000)
//app.listen(process.argv[2] || 80)






