const ANIMALS = [
  { id: 'asian-elephant', name: '亚洲象', icon: '🐘', route: 'elephant', routeLabel: '个体识别', note: '识别亚洲象个体', selectable: true },
  { id: 'giant-panda', name: '大熊猫', icon: '🐼', route: 'universal', routeLabel: '种类识别', note: '识别动物种类', selectable: true },
  { id: 'giraffe', name: '长颈鹿', icon: '🦒', route: 'universal', routeLabel: '种类识别', note: '识别动物种类', selectable: true },
  { id: 'chimpanzee', name: '黑猩猩', icon: '🦍', route: 'universal', routeLabel: '种类识别', note: '识别动物种类', selectable: true },
  { id: 'proboscis-monkey', name: '长鼻猴', icon: '🐒', route: 'universal', routeLabel: '种类识别', note: '识别动物种类', selectable: true },
  { id: 'koala', name: '考拉', icon: '🐨', route: 'universal', routeLabel: '种类识别', note: '识别动物种类', selectable: true },
  { id: 'white-tiger', name: '白虎', icon: '🐯', route: 'universal', routeLabel: '种类识别', note: '识别动物种类', selectable: true },
  { id: 'lion', name: '狮子', icon: '🦁', route: 'universal', routeLabel: '种类识别', note: '识别动物种类', selectable: true },
  { id: 'flamingo', name: '火烈鸟', icon: '🦩', route: 'universal', routeLabel: '种类识别', note: '识别动物种类', selectable: true },
  { id: 'white-rhino', name: '白犀牛', icon: '🦏', route: 'universal', routeLabel: '种类识别', note: '识别动物种类', selectable: true }
]

function findAnimal(id) {
  return ANIMALS.find((item) => item.id === id) || ANIMALS[0]
}

module.exports = { ANIMALS, findAnimal }
