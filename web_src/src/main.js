import Vue from 'vue';
import App from './App.vue';
Vue.config.productionTip = false;
import ElementUI from 'element-ui';
import 'element-ui/lib/theme-chalk/index.css';
import router from './router/index.js';
import axios from 'axios';
import VueCookies from 'vue-cookies';
import echarts from 'echarts';

import VueClipboard from 'vue-clipboard2';
import { Notification } from 'element-ui';
import Fingerprint2 from 'fingerprintjs2';

// ����ΨһID
Fingerprint2.get(function(components) {
  const values = components.map(function(component,index) {
    if (index === 0) { //��΢���������UA��wifi��4G�������滻�ɿ�,��Ȼ�л������ID��һ��
      return component.value.replace(/\bNetType\/\w+\b/, '');
    }
    return component.value;
  })
  //console.log(values)  //ʹ�õ��������Ϣnpm 
  // ��������id
  let port = window.location.port;
  console.log(port);
  const fingerPrint = Fingerprint2.x64hash128(values.join(port), 31)
  Vue.prototype.$browserId = fingerPrint;
  console.log("Ψһ��ʶ�룺" + fingerPrint);
});

Vue.use(VueClipboard);
Vue.use(ElementUI);
Vue.use(VueCookies);
Vue.prototype.$axios = axios;
Vue.prototype.$notify = Notification;

axios.defaults.baseURL = (process.env.NODE_ENV === 'development') ? process.env.BASE_API : "";

Vue.prototype.$cookies.config(60*30);


new Vue({
	router: router,
	render: h => h(App),
}).$mount('#app')
