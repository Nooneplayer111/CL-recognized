        // //方式一: 写入浏览器的body区域
        // document.write("Hello JS (document.write)");

        // //方式二: 弹出框
        // window.alert("Hello JS (window.alert)");

        // //方式三: 控制台
        // console.log("Hello JS (console.log)")

        // var add = (a,b) => {
        //   return a+b;
        // }

        // let result = add(10,20);
        // alert(result);

        // let user = {
        //   name:"Tom",
        //   age:10,
        //   gender:"男",
        //   sing(){
        //     console.log("风到这里就是粘");
        //   }
        // }

        // alert(JSON.stringify(user));
        // let userJson = '{"name": "heima", "age": 18}';
        // alert(JSON.parse(userJson).name);
        // user.sing();

        let hs = document.querySelectorAll('h1');

        hs[0].innerHTML = '修改后的文本'

        document.querySelector('#btn1').addEventListener('click',()=>{
          alert("按钮1被点击了");
        })

        