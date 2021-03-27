# Median Filter
### Introducción:

En este trabajo se utilizó scala para crear una aplicación donde el usuario puede subir una foto en formato jpg en donde usando el median filter de scala se procesa para disminuir el ruido de la imagen. La imagen será procesada por dos servidores diferentes donde se utilizan diferentes métodos para procesar la imagen, uno de estos es creara una implementación serial y la otra usara colecciones paralelas. Ambas fotos serán devueltas al usuario luego de ser procesadas.

### Filtro:
Para cada pixel de la imagen se buscará los 8 pixeles adyacentes en todas las direcciones y se busca en el registro de valores de RGB el valor más repetido. Este proceso continuará hasta que se haya procesado cada pixel y para cada píxel se le asignará un nuevo valor dependiendo del cálculo que haya obtenido.



## Imagenes 
### Foto de prueba 
![alt text](https://github.com/heckio/median-filter/blob/master/src/main/imgs/test.jpg)
### Serial
![alt text](https://github.com/heckio/median-filter/blob/master/src/main/imgs/linearIMG.jpg)
### Paralelo
![alt text](https://github.com/heckio/median-filter/blob/master/src/main/imgs/concurrentIMG.jpg)


## Tiempo de ejecicion
### Serial
```bash
[debug] Load.apply: finalTransforms took 12.9815ms
```
### Paralelo
```bash
[debug] Load.apply: finalTransforms took 5.1256ms
```
