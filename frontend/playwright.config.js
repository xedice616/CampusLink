import {defineConfig} from '@playwright/test';
export default defineConfig({testDir:'./tests',fullyParallel:false,workers:1,timeout:30000,use:{baseURL:'http://127.0.0.1:5191',headless:true,screenshot:'only-on-failure'},webServer:{command:'npm.cmd run dev -- --host 127.0.0.1 --port 5191 --strictPort',url:'http://127.0.0.1:5191',reuseExistingServer:false},reporter:'list'});
