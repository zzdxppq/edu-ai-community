import { createRouter, createWebHistory, type RouteRecordRaw } from 'vue-router';
import HomeView from '@/views/home/HomeView.vue';

const routes: RouteRecordRaw[] = [
  {
    path: '/',
    name: 'Home',
    component: HomeView,
  },
];

const router = createRouter({
  history: createWebHistory(),
  routes,
});

export default router;
